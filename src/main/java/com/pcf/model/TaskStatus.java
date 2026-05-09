package com.pcf.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskStatus {
    PENDING(0),
    DOWNLOADING(1),
    GENERATING_COPY(2),
    RENDERING(3),
    PUBLISHING(4),
    COMPLETED(5),
    FAILED(-1);

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
