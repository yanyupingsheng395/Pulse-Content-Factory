package com.pcf.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "content_tasks",
        uniqueConstraints = @UniqueConstraint(name = "uk_content_tasks_share_url", columnNames = "share_url")
)
public class ContentTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String platform;

    @Column(name = "share_url", nullable = false, length = 2048)
    private String shareUrl;

    @Column(name = "video_id", length = 128)
    private String videoId;

    @Column(name = "title_original", length = 2000)
    private String titleOriginal;

    @Column(name = "title_generated", length = 2000)
    private String titleGenerated;

    /** DNA：原标题（yt-dlp 等） */
    @Column(name = "dna_title", length = 2000)
    private String dnaTitle;

    /** DNA：描述/简介文案 */
    @Column(name = "dna_description", length = 4000)
    private String dnaDescription;

    /** 仿写口播/脚本 */
    @Column(name = "script_rewritten", length = 8000)
    private String scriptRewritten;

    /** 英文关键词，供 MPT video_terms */
    @Column(name = "keywords_en", length = 2000)
    private String keywordsEn;

    /** MoneyPrinterTurbo task_id */
    @Column(name = "external_job_id", length = 64)
    private String externalJobId;

    /** MPT 返回的可访问视频 URL */
    @Column(name = "remote_video_url", length = 4000)
    private String remoteVideoUrl;

    /** 本任务使用的配音（可覆盖默认） */
    @Column(name = "voice_name", length = 256)
    private String voiceName;

    @Column(nullable = false)
    private Integer status = TaskStatus.PENDING.getCode();

    @Column(name = "local_path", length = 2048)
    private String localPath;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
