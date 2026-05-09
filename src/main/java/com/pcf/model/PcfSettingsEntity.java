package com.pcf.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

/**
 * 单行配置（id 固定为 1），持久化覆盖 application.yml 默认值。
 */
@Getter
@Setter
@Entity
@Table(name = "pcf_settings")
public class PcfSettingsEntity {

    public static final Long SINGLETON_ID = 1L;

    @Id
    @Column(nullable = false)
    private Long id = SINGLETON_ID;

    @Column(name = "deepseek_base_url", length = 512)
    private String deepseekBaseUrl;

    @Column(name = "deepseek_api_key", length = 512)
    private String deepseekApiKey;

    @Column(name = "deepseek_model", length = 128)
    private String deepseekModel;

    @Column(name = "siliconflow_base_url", length = 512)
    private String siliconflowBaseUrl;

    @Column(name = "siliconflow_api_key", length = 512)
    private String siliconflowApiKey;

    @Column(name = "siliconflow_model", length = 128)
    private String siliconflowModel;

    /** MoneyPrinterTurbo API 根地址，如 http://127.0.0.1:8080 */
    @Column(name = "mpt_base_url", length = 512)
    private String mptBaseUrl;

    @Column(name = "local_footage_dir", length = 2048)
    private String localFootageDir;

    @Column(name = "cookie_douyin", length = 8000)
    private String cookieDouyin;

    @Column(name = "cookie_xiaohongshu", length = 8000)
    private String cookieXiaohongshu;

    @Column(name = "default_voice_name", length = 256)
    private String defaultVoiceName;

    /** 如 9:16 */
    @Column(name = "default_video_aspect", length = 32)
    private String defaultVideoAspect;

    /** LOCAL 或 TURBO */
    @Column(name = "render_mode", length = 32)
    private String renderMode;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
