package com.pcf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    private final SettingsService settingsService;
    private final ObjectMapper objectMapper;

    private final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(Duration.ofMinutes(3))
            .build();

    public AiGenerationResult analyzeAndGenerate(String originalTitle, String commentSample) throws IOException {
        if (StringUtil.isBlank(settingsService.effectiveDeepseekApiKey())) {
            return new AiGenerationResult(
                    "{\"note\":\"skipped_no_api_key\"}",
                    originalTitle == null ? "未配置 DeepSeek" : originalTitle + "（未调用模型）",
                    "请配置 DEEPSEEK_API_KEY 或在设置页填写 API Key 后重新执行任务。"
            );
        }

        String url = trimSlash(settingsService.effectiveDeepseekBaseUrl()) + "/v1/chat/completions";
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
        root.put("model", settingsService.effectiveDeepseekModel());
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
                .header("Authorization", "Bearer " + settingsService.effectiveDeepseekApiKey())
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

    /**
     * Turbo 流水线：根据 DNA 生成仿写标题、口播稿、英文关键词（供 MoneyPrinterTurbo video_terms）。
     */
    public AiCloneResult cloneRewrite(String dnaTitle, String dnaDescription) throws IOException {
        if (StringUtil.isBlank(settingsService.effectiveDeepseekApiKey())) {
            return new AiCloneResult(
                    dnaTitle == null ? "" : dnaTitle,
                    "请先在设置中配置 DeepSeek API Key。",
                    "nature, video, short"
            );
        }
        String url = trimSlash(settingsService.effectiveDeepseekBaseUrl()) + "/v1/chat/completions";
        String userContent = String.format(
                "你是短视频编导。根据下面「原视频标题」和「原简介/文案」完成仿写，只输出一个 JSON（不要 Markdown）。\n"
                        + "字段要求：\n"
                        + "generated_title：中文标题，50 字以内，意思贴近但不要抄袭原句。\n"
                        + "generated_script：中文口播稿，80-140 字，适合短视频配音。\n"
                        + "video_keywords_en：字符串，3-8 个英文关键词，用英文逗号分隔，用于素材搜索（如 nature,sunset,travel）。\n"
                        + "\n原视频标题：%s\n原简介/文案：%s\n",
                dnaTitle == null ? "" : dnaTitle,
                dnaDescription == null ? "" : dnaDescription
        );
        String sys = "只输出合法 JSON，键为 generated_title, generated_script, video_keywords_en。";
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", settingsService.effectiveDeepseekModel());
        ArrayNode messages = root.putArray("messages");
        ObjectNode sysN = messages.addObject();
        sysN.put("role", "system");
        sysN.put("content", sys);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", userContent);
        root.put("temperature", 0.7);

        Request req = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + settingsService.effectiveDeepseekApiKey())
                .post(RequestBody.create(JSON, root.toString().getBytes(StandardCharsets.UTF_8)))
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful() || res.body() == null) {
                throw new IOException("DeepSeek HTTP " + res.code() + " " + res.message());
            }
            JsonNode tree = objectMapper.readTree(res.body().string());
            String content = tree.path("choices").path(0).path("message").path("content").asText("");
            JsonNode parsed = tryParseJsonContent(content);
            if (parsed != null && parsed.has("generated_script")) {
                return new AiCloneResult(
                        parsed.path("generated_title").asText(dnaTitle),
                        parsed.path("generated_script").asText(""),
                        parsed.path("video_keywords_en").asText("video,content")
                );
            }
            return new AiCloneResult(dnaTitle, content, "video,short");
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

    @Getter
    @RequiredArgsConstructor
    public static class AiCloneResult {
        private final String generatedTitle;
        private final String generatedScript;
        private final String videoKeywordsEn;
    }
}
