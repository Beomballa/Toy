package com.section.common.base.entity.type;

import java.util.Arrays;

public enum AdminOperationTaskPriority {
    HIGH("HIGH", "높음"),
    MEDIUM("MEDIUM", "보통"),
    LOW("LOW", "낮음");

    private final String code;
    private final String label;

    AdminOperationTaskPriority(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static AdminOperationTaskPriority fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown task priority: " + code));
    }
}
