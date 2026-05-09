package com.pcf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pcf.config.PcfProperties;
import com.pcf.util.StringUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AIService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final PcfProperties properties;
    private final ObjectMapper objectMapper;

    private final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(Duration.ofMinutes(3))
            .build();

    public AiGenerationResult analyzeAndGenerate(String originalTitle, String commentSample) throws IOException {
        if (StringUtil.isBlank(properties.getDeepseek().getApiKey())) {
            return new AiGenerationResult(
                    "{\"note\":\"skipped_no_api_key\"}",
                    originalTitle == null ? "未配置 DeepSeek" : originalTitle + "（未调用模型）",
                    "请配置 DEEPSEEK_API_KEY 后重新执行任务。"
            );
        }

        String url = trimSlash(properties.getDeepseek().getBaseUrl()) + "/v1/chat/completions";
        String userContent = String.format(
                "你是短视频内容策划。请基于以下信息完成两件事，并只输出一个 JSON 对象（不要 Markdown）：\n"
                        + "1) analysis：分析原内容的钩子（hook）、痛点（pain）、叙事结构（structure）。\n"
                        + "2) generated_title：在原逻辑下重写标题，50 字以内，避免抄袭措辞。\n"
                        + "3) generated_script：口播稿，约 80-120 字，结构与原内容一致但表达去重。\n"
                        + "\n"
                        + "原视频标题：%s\n"
                        + "评论/补充文本（可为空）：%s\n",
                originalTitle == null ? "" : originalTitle,
                commentSample == null ? "" : commentSample
        );

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getDeepseek().getModel());
        ArrayNode messages = root.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", "只输出合法 JSON，键为 analysis(对象含 hook,pain,structure), generated_title, generated_script。");
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", userContent);
        root.put("temperature", 0.7);

        Request req = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + properties.getDeepseek().getApiKey())
                .post(RequestBody.create(JSON, root.toString().getBytes(StandardCharsets.UTF_8)))
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful() || res.body() == null) {
                throw new IOException("DeepSeek HTTP " + res.code() + " " + res.message());
            }
            JsonNode tree = objectMapper.readTree(res.body().string());
            String content = tree.path("choices").path(0).path("message").path("content").asText("");
            JsonNode parsed = tryParseJsonContent(content);
            if (parsed != null && parsed.has("generated_title")) {
                String analysis = parsed.path("analysis").toString();
                String gt = parsed.path("generated_title").asText("");
                String gs = parsed.path("generated_script").asText("");
                return new AiGenerationResult(analysis, gt, gs);
            }
            return new AiGenerationResult("{}", content, content);
        }
    }

    private JsonNode tryParseJsonContent(String content) {
        if (StringUtil.isBlank(content)) {
            return null;
        }
        String t = content.trim();
        try {
            return objectMapper.readTree(t);
        } catch (Exception e) {
            int i = t.indexOf('{');
            int j = t.lastIndexOf('}');
            if (i >= 0 && j > i) {
                try {
                    return objectMapper.readTree(t.substring(i, j + 1));
                } catch (Exception ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    private static String trimSlash(String base) {
        if (base == null) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @Getter
    @RequiredArgsConstructor
    public static class AiGenerationResult {
        private final String analysisJson;
        private final String generatedTitle;
        private final String generatedScript;
    }
}
