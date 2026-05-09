package com.pcf.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "pcf")
public class PcfProperties {

    private String workDir = System.getProperty("user.home") + "/.pcf";
    private String ytDlpPath = "yt-dlp";
    private String ffmpegPath = "ffmpeg";
    private String localFootageDir = "C:/MyFootage";
    private DeepSeek deepseek = new DeepSeek();
    private SiliconFlow siliconflow = new SiliconFlow();
    private Pexels pexels = new Pexels();
    private Playwright playwright = new Playwright();
    private Publish publish = new Publish();
    /**
     * 是否执行真实浏览器发布（默认关闭，避免误操作）。
     */
    private boolean publishEnabled = false;

    @Data
    public static class DeepSeek {
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "";
        private String model = "deepseek-chat";
    }

    @Data
    public static class SiliconFlow {
        private String baseUrl = "https://api.siliconflow.cn";
        private String apiKey = "";
        private String ttsPath = "/v1/audio/speech";
        private String model = "";
    }

    @Data
    public static class Pexels {
        private String apiKey = "";
    }

    @Data
    public static class Playwright {
        private String userDataDir = "";
        private boolean headless = false;
    }

    @Data
    public static class Publish {
        private long minDelayMs = 2000;
        private long maxDelayMs = 5000;
        private Douyin douyin = new Douyin();

        @Data
        public static class Douyin {
            private String creatorUrl = "https://creator.douyin.com/";
            private String uploadButtonSelector = "";
            private String titleInputSelector = "";
            private String topicInputSelector = "";
        }
    }
}
