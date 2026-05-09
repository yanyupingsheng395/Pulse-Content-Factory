package com.pcf.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FFmpegCommandBuilder {

    private final String ffmpegPath;

    public FFmpegCommandBuilder(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public List<String> extractMp3(Path inputVideo, Path outputMp3) {
        return Arrays.asList(
                ffmpegPath,
                "-y",
                "-i", inputVideo.toString(),
                "-vn",
                "-acodec", "libmp3lame",
                outputMp3.toString()
        );
    }

    public List<String> captureFrame(Path inputVideo, Path outputPng, double seconds) {
        return Arrays.asList(
                ffmpegPath,
                "-y",
                "-ss", String.format("%.3f", seconds),
                "-i", inputVideo.toString(),
                "-frames:v", "1",
                "-q:v", "2",
                outputPng.toString()
        );
    }

    /**
     * 轻度画面微调 + 将 TTS 音轨作为主音频（示例管线，可按业务替换）。
     */
    public List<String> muxVideoWithTts(Path inputVideo, Path ttsAudio, Path outputMp4, long taskId) {
        Random rnd = new Random(taskId);
        double cropFactor = 0.88 + rnd.nextDouble() * 0.04;
        String vf = String.format(
                "crop=iw*%.4f:ih*%.4f,eq=brightness=0.02:contrast=1.02",
                cropFactor,
                cropFactor
        );
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath);
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(inputVideo.toString());
        cmd.add("-i");
        cmd.add(ttsAudio.toString());
        cmd.add("-vf");
        cmd.add(vf);
        cmd.add("-map");
        cmd.add("0:v:0");
        cmd.add("-map");
        cmd.add("1:a:0");
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-shortest");
        cmd.add(outputMp4.toString());
        return cmd;
    }

    public List<String> copyAsFinal(Path inputVideo, Path outputMp4) {
        return Arrays.asList(
                ffmpegPath,
                "-y",
                "-i", inputVideo.toString(),
                "-c", "copy",
                outputMp4.toString()
        );
    }
}
