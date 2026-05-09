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
    /**
     * LOCAL：本机 FFmpeg+TTS；TURBO：MoneyPrinterTurbo API。
     */
    private String renderMode = "TURBO";
    private DeepSeek deepseek = new DeepSeek();
    private SiliconFlow siliconflow = new SiliconFlow();
    private Pexels pexels = new Pexels();
    private Playwright playwright = new Playwright();
    private MoneyprinterTurbo moneyprinterTurbo = new MoneyprinterTurbo();
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
    public static class MoneyprinterTurbo {
        /** 与 MPT FastAPI 监听端口一致，见 MPT 配置 listen_port */
        private String baseUrl = "http://127.0.0.1:8080";
        private String defaultVoiceName = "zh-CN-XiaoxiaoNeural-Female";
        private String defaultVideoAspect = "9:16";
        /** 轮询间隔 ms */
        private long pollIntervalMs = 2000;
        /** 最大轮询次数 */
        private int pollMaxAttempts = 600;
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
