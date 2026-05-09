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

    @Transactional
    public ContentTask createFromText(String rawText, String platform, String titleOriginal) {
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
            runPipeline(task);
        } catch (Exception e) {
            log.error("任务 {} 失败", taskId, e);
            markFailed(taskId, e);
        }
    }

    private void runPipeline(ContentTask task) throws Exception {
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
