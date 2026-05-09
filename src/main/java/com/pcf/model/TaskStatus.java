package com.pcf.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskStatus {
    PENDING(0),
    /** 本机模式：下载素材 */
    DOWNLOADING(1),
    /** 本机模式：AI 文案 */
    GENERATING_COPY(2),
    /** 本机模式：FFmpeg 渲染 */
    RENDERING(3),
    PUBLISHING(4),
    COMPLETED(5),
    FAILED(-1),
    /** Turbo：仅拉取元数据 / DNA */
    DNA_EXTRACTING(10),
    /** Turbo：DeepSeek 仿写 + 关键词 */
    AI_REWRITING(11),
    /** Turbo：MoneyPrinterTurbo 渲染中 */
    TURBO_RENDERING(12),
    /** 成片已就绪，待手动发布 */
    READY_TO_PUBLISH(13);

    private final int code;

    public static TaskStatus fromCode(int code) {
        for (TaskStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return PENDING;
    }
}
