package com.pcf.service;

import com.pcf.config.PcfProperties;
import com.pcf.util.FFmpegCommandBuilder;
import com.pcf.util.ProcessRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final PcfProperties properties;
    private final DownloadService downloadService;

    public Path extractAudioMp3(long taskId, Path inputVideo) throws IOException, InterruptedException {
        Path out = Paths.get(properties.getWorkDir(), "temp", "raw", String.valueOf(taskId), "original_bgm.mp3");
        Files.createDirectories(out.getParent());
        FFmpegCommandBuilder b = new FFmpegCommandBuilder(properties.getFfmpegPath());
        ProcessRunner.Result r = ProcessRunner.run(b.extractMp3(inputVideo, out), 600);
        r.requireSuccess();
        return out;
    }

    public void captureKeyframes(long taskId, Path inputVideo) throws IOException, InterruptedException {
        Path dir = Paths.get(properties.getWorkDir(), "temp", "raw", String.valueOf(taskId), "frames");
        Files.createDirectories(dir);
        FFmpegCommandBuilder b = new FFmpegCommandBuilder(properties.getFfmpegPath());
        double[] pts = {0.5, 5.0, 12.0};
        for (int i = 0; i < pts.length; i++) {
            Path png = dir.resolve("frame_" + i + ".png");
            ProcessRunner.Result r = ProcessRunner.run(b.captureFrame(inputVideo, png, pts[i]), 120);
            r.requireSuccess();
        }
    }

    public Path downloadAndPrepare(long taskId, String shareUrl) throws IOException, InterruptedException {
        Path video = downloadService.downloadVideo(taskId, shareUrl);
        extractAudioMp3(taskId, video);
        try {
            captureKeyframes(taskId, video);
        } catch (Exception e) {
            // 短视频可能不足固定时间点，失败不阻断主流程
        }
        return video;
    }
}
