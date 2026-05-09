package com.pcf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pcf.config.PcfProperties;
import com.pcf.util.FFmpegCommandBuilder;
import com.pcf.util.ProcessRunner;
import com.pcf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RenderService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final PcfProperties properties;
    private final ObjectMapper objectMapper;

    private final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(Duration.ofMinutes(5))
            .build();

    public Path renderFinal(long taskId, Path rawVideo, String ttsText) throws IOException, InterruptedException {
        Path out = Paths.get(properties.getWorkDir(), "output", taskId + "_final.mp4");
        Files.createDirectories(out.getParent());

        Path tts = Paths.get(properties.getWorkDir(), "temp", "raw", String.valueOf(taskId), "tts.mp3");
        Files.createDirectories(tts.getParent());

        boolean ttsOk = synthesizeSpeech(ttsText, tts);
        FFmpegCommandBuilder b = new FFmpegCommandBuilder(properties.getFfmpegPath());
        if (ttsOk && Files.exists(tts) && Files.size(tts) > 0) {
            ProcessRunner.Result r = ProcessRunner.run(b.muxVideoWithTts(rawVideo, tts, out, taskId), 900);
            r.requireSuccess();
        } else {
            ProcessRunner.Result r = ProcessRunner.run(b.copyAsFinal(rawVideo, out), 900);
            r.requireSuccess();
        }
        return out;
    }

    private boolean synthesizeSpeech(String text, Path outMp3) throws IOException {
        if (StringUtil.isBlank(text)) {
            return false;
        }
        if (StringUtil.isBlank(properties.getSiliconflow().getApiKey())) {
            return false;
        }
        String base = trimSlash(properties.getSiliconflow().getBaseUrl());
        String path = properties.getSiliconflow().getTtsPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String url = base + path;

        ObjectNode body = objectMapper.createObjectNode();
        if (!StringUtil.isBlank(properties.getSiliconflow().getModel())) {
            body.put("model", properties.getSiliconflow().getModel());
        }
        body.put("input", text);

        Request req = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + properties.getSiliconflow().getApiKey())
                .post(RequestBody.create(JSON, body.toString().getBytes(StandardCharsets.UTF_8)))
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful() || res.body() == null) {
                return false;
            }
            byte[] bytes = res.body().bytes();
            if (bytes.length == 0) {
                return false;
            }
            Files.write(outMp3, bytes);
            return true;
        }
    }

    private static String trimSlash(String base) {
        if (base == null) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
