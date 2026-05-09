package com.pcf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pcf.config.PcfProperties;
import com.pcf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 对接 harry0703/MoneyPrinterTurbo：POST /videos ，GET /tasks/{task_id}。
 */
@Component
@RequiredArgsConstructor
public class MoneyPrinterTurboClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    /** MPT const.TASK_STATE_COMPLETE */
    private static final int STATE_COMPLETE = 1;
    /** MPT const.TASK_STATE_FAILED */
    private static final int STATE_FAILED = -1;

    private final SettingsService settingsService;
    private final PcfProperties pcfProperties;
    private final ObjectMapper objectMapper;

    private final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(Duration.ofMinutes(10))
            .readTimeout(Duration.ofMinutes(10))
            .build();

    public String createVideoJob(String videoSubject, String videoScript, String videoTermsEn,
                                 String voiceName, String videoAspect) throws IOException {
        String base = trimBase(settingsService.effectiveMptBaseUrl());
        ObjectNode body = objectMapper.createObjectNode();
        body.put("video_subject", videoSubject);
        if (!StringUtil.isBlank(videoScript)) {
            body.put("video_script", videoScript);
        }
        if (!StringUtil.isBlank(videoTermsEn)) {
            body.put("video_terms", videoTermsEn);
        }
        body.put("video_aspect", StringUtil.isBlank(videoAspect) ? "9:16" : videoAspect);
        body.put("voice_name", voiceName == null ? "" : voiceName);

        Request req = new Request.Builder()
                .url(base + "/videos")
                .post(RequestBody.create(JSON, body.toString().getBytes(StandardCharsets.UTF_8)))
                .build();
        try (Response res = http.newCall(req).execute()) {
            String respBody = res.body() != null ? res.body().string() : "";
            if (!res.isSuccessful()) {
                throw new IOException("MPT POST /videos HTTP " + res.code() + " " + respBody);
            }
            JsonNode root = objectMapper.readTree(respBody);
            JsonNode data = root.path("data");
            String taskId = data.path("task_id").asText(null);
            if (StringUtil.isBlank(taskId)) {
                throw new IOException("MPT 响应缺少 data.task_id: " + respBody);
            }
            return taskId;
        }
    }

    /**
     * 轮询直到完成或失败；完成时返回第一条成片 URL。
     */
    public String pollUntilComplete(String mptTaskId) throws IOException, InterruptedException {
        String base = trimBase(settingsService.effectiveMptBaseUrl());
        long interval = pcfProperties.getMoneyprinterTurbo().getPollIntervalMs();
        int max = pcfProperties.getMoneyprinterTurbo().getPollMaxAttempts();
        Request req = new Request.Builder()
                .url(base + "/tasks/" + mptTaskId)
                .get()
                .build();
        for (int i = 0; i < max; i++) {
            try (Response res = http.newCall(req).execute()) {
                String respBody = res.body() != null ? res.body().string() : "";
                if (!res.isSuccessful()) {
                    throw new IOException("MPT GET /tasks/ HTTP " + res.code() + " " + respBody);
                }
                JsonNode root = objectMapper.readTree(respBody);
                JsonNode data = root.path("data");
                int state = data.path("state").asInt(0);
                if (state == STATE_FAILED) {
                    throw new IOException("MoneyPrinterTurbo 任务失败: " + respBody);
                }
                if (state == STATE_COMPLETE) {
                    JsonNode videos = data.path("videos");
                    if (videos.isArray() && videos.size() > 0) {
                        return videos.get(0).asText("");
                    }
                    JsonNode combined = data.path("combined_videos");
                    if (combined.isArray() && combined.size() > 0) {
                        return combined.get(0).asText("");
                    }
                    throw new IOException("MPT 已完成但无 videos: " + respBody);
                }
            }
            TimeUnit.MILLISECONDS.sleep(Math.max(500L, interval));
        }
        throw new IOException("MoneyPrinterTurbo 轮询超时 task=" + mptTaskId);
    }

    public void downloadVideo(String videoUrl, Path targetFile) throws IOException {
        Request req = new Request.Builder().url(videoUrl).get().build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful() || res.body() == null) {
                throw new IOException("下载成片失败 HTTP " + res.code());
            }
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, res.body().bytes());
        }
    }

    private static String trimBase(String base) {
        if (base == null) {
            return "http://127.0.0.1:8080";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
