package com.pcf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcf.config.PcfProperties;
import com.pcf.util.ProcessRunner;
import com.pcf.util.StringUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DnaExtractionService {

    private final PcfProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 使用 yt-dlp 仅拉取元数据，不下载视频文件。
     */
    public DnaResult extractMetadata(String pageUrl) throws IOException, InterruptedException {
        List<String> cmd = Arrays.asList(
                properties.getYtDlpPath(),
                "--dump-json",
                "--skip-download",
                "--no-warnings",
                pageUrl
        );
        ProcessRunner.Result r = ProcessRunner.run(cmd, 180);
        r.requireSuccess();
        String out = r.getOutput();
        if (StringUtil.isBlank(out)) {
            return new DnaResult("", "");
        }
        String line = out.split("\n", 2)[0].trim();
        JsonNode root = objectMapper.readTree(line);
        String title = root.path("title").asText("");
        String desc = root.path("description").asText("");
        if (StringUtil.isBlank(desc)) {
            desc = root.path("track").asText("");
        }
        return new DnaResult(title, desc);
    }

    @Getter
    @RequiredArgsConstructor
    public static class DnaResult {
        private final String title;
        private final String description;
    }
}
