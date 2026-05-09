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
