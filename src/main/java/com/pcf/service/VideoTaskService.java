package com.pcf.service;

import com.pcf.config.PcfProperties;
import com.pcf.dao.ContentTaskRepository;
import com.pcf.model.ContentTask;
import com.pcf.model.TaskStatus;
import com.pcf.util.LinkParserUtil;
import com.pcf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class VideoTaskService {

    private static final Logger log = LoggerFactory.getLogger(VideoTaskService.class);

    private final ContentTaskRepository taskRepository;
    private final AnalysisService analysisService;
    private final AIService aiService;
    private final RenderService renderService;
    private final PublishService publishService;
    private final PcfProperties properties;
    private final SettingsService settingsService;
    private final DnaExtractionService dnaExtractionService;
    private final MoneyPrinterTurboClient moneyPrinterTurboClient;

    @Transactional
    public ContentTask createFromText(String rawText, String platform, String titleOriginal, String voiceName) {
        String url = LinkParserUtil.firstUrlOrNull(rawText);
        if (url == null) {
            throw new IllegalArgumentException("未在文本中解析到有效 http(s) 链接");
        }
        taskRepository.findByShareUrl(url).ifPresent(t -> {
            throw new IllegalStateException("该链接已存在，任务 id=" + t.getId());
        });
        ContentTask task = new ContentTask();
        task.setPlatform(platform == null || StringUtil.isBlank(platform) ? "unknown" : platform);
        task.setShareUrl(url);
        task.setTitleOriginal(titleOriginal);
        if (!StringUtil.isBlank(voiceName)) {
            task.setVoiceName(voiceName.trim());
        }
        task.setStatus(TaskStatus.PENDING.getCode());
        return taskRepository.save(task);
    }

    @Async("taskExecutor")
    public void processTaskAsync(Long taskId) {
        ContentTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        try {
            if (settingsService.isTurboMode()) {
                runTurboPipeline(task);
            } else {
                runLocalPipeline(task);
            }
        } catch (Exception e) {
            log.error("任务 {} 失败", taskId, e);
            markFailed(taskId, e);
        }
    }

    @Async("taskExecutor")
    public void publishManualAsync(Long taskId) {
        ContentTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        try {
            if (StringUtil.isBlank(task.getLocalPath())) {
                throw new IllegalStateException("无本地成片路径，无法上传发布");
            }
            Path file = Paths.get(task.getLocalPath());
            updateStatus(taskId, TaskStatus.PUBLISHING);
            String title = task.getTitleGenerated() != null ? task.getTitleGenerated() : task.getTitleOriginal();
            publishService.publishManual(file, title == null ? "" : title, "");
            updateStatus(taskId, TaskStatus.COMPLETED);
        } catch (Exception e) {
            log.error("手动发布 {} 失败", taskId, e);
            markFailed(taskId, e);
        }
    }

    private void runTurboPipeline(ContentTask task) throws Exception {
        Long id = task.getId();
        updateStatus(id, TaskStatus.DNA_EXTRACTING);
        DnaExtractionService.DnaResult dna = dnaExtractionService.extractMetadata(task.getShareUrl());
        patchDna(id, dna.getTitle(), dna.getDescription());

        String dnaTitle = dna.getTitle();
        if (!StringUtil.isBlank(task.getTitleOriginal())) {
            dnaTitle = task.getTitleOriginal();
        }

        updateStatus(id, TaskStatus.AI_REWRITING);
        AIService.AiCloneResult clone = aiService.cloneRewrite(dnaTitle, dna.getDescription());
        patchAiClone(id, clone.getGeneratedTitle(), clone.getGeneratedScript(), clone.getVideoKeywordsEn());

        String voice = task.getVoiceName();
        if (StringUtil.isBlank(voice)) {
            voice = settingsService.effectiveDefaultVoice();
        }
        String aspect = settingsService.effectiveDefaultAspect();

        ContentTask fresh = taskRepository.findById(id).orElse(task);
        String subject = fresh.getTitleGenerated() != null ? fresh.getTitleGenerated() : clone.getGeneratedTitle();
        String script = fresh.getScriptRewritten() != null ? fresh.getScriptRewritten() : clone.getGeneratedScript();
        String terms = fresh.getKeywordsEn();

        updateStatus(id, TaskStatus.TURBO_RENDERING);
        String mptTaskId = moneyPrinterTurboClient.createVideoJob(subject, script, terms, voice, aspect);
        patchExternalJob(id, mptTaskId);

        String videoUrl = moneyPrinterTurboClient.pollUntilComplete(mptTaskId);
        patchRemoteVideoUrl(id, videoUrl);

        Path localOut = Paths.get(properties.getWorkDir(), "output", id + "_turbo.mp4");
        try {
            moneyPrinterTurboClient.downloadVideo(videoUrl, localOut);
            patchLocalPath(id, localOut.toString());
        } catch (Exception ex) {
            log.warn("下载 Turbo 成片到本地失败，仍保留远程 URL: {}", ex.getMessage());
        }

        updateStatus(id, TaskStatus.READY_TO_PUBLISH);
    }

    private void runLocalPipeline(ContentTask task) throws Exception {
        Long id = task.getId();
        updateStatus(id, TaskStatus.DOWNLOADING);
        Path raw = analysisService.downloadAndPrepare(id, task.getShareUrl());

        updateStatus(id, TaskStatus.GENERATING_COPY);
        AIService.AiGenerationResult ai = aiService.analyzeAndGenerate(
                task.getTitleOriginal() == null ? "" : task.getTitleOriginal(),
                ""
        );
        patchGeneratedTitle(id, ai.getGeneratedTitle());

        updateStatus(id, TaskStatus.RENDERING);
        Path finalVideo = renderService.renderFinal(id, raw, ai.getGeneratedScript());
        patchLocalPath(id, finalVideo.toString());

        if (properties.isPublishEnabled()) {
            updateStatus(id, TaskStatus.PUBLISHING);
        }
        publishService.publishIfEnabled(finalVideo, ai.getGeneratedTitle(), "");
        updateStatus(id, TaskStatus.COMPLETED);
    }

    private void patchDna(Long id, String title, String desc) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setDnaTitle(title);
            t.setDnaDescription(desc);
            taskRepository.save(t);
        });
    }

    private void patchAiClone(Long id, String genTitle, String script, String keywordsEn) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setTitleGenerated(genTitle);
            t.setScriptRewritten(script);
            t.setKeywordsEn(keywordsEn);
            taskRepository.save(t);
        });
    }

    private void patchExternalJob(Long id, String jobId) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setExternalJobId(jobId);
            taskRepository.save(t);
        });
    }

    private void patchRemoteVideoUrl(Long id, String url) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setRemoteVideoUrl(url);
            taskRepository.save(t);
        });
    }

    private void patchGeneratedTitle(Long id, String generatedTitle) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setTitleGenerated(generatedTitle);
            taskRepository.save(t);
        });
    }

    private void patchLocalPath(Long id, String path) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setLocalPath(path);
            taskRepository.save(t);
        });
    }

    private void updateStatus(Long id, TaskStatus status) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setStatus(status.getCode());
            taskRepository.save(t);
        });
    }

    private void markFailed(Long id, Exception e) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setStatus(TaskStatus.FAILED.getCode());
            t.setRetryCount((t.getRetryCount() == null ? 0 : t.getRetryCount()) + 1);
            String msg = e.getMessage();
            if (msg != null && msg.length() > 2000) {
                msg = msg.substring(0, 2000);
            }
            t.setErrorMessage(msg);
            taskRepository.save(t);
        });
    }
}
