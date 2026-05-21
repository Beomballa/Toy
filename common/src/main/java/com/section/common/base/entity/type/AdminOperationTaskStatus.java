package com.section.common.base.entity.type;

import java.util.Arrays;

public enum AdminOperationTaskStatus {
    TODO("TODO", "대기"),
    IN_PROGRESS("IN_PROGRESS", "진행중"),
    DONE("DONE", "완료"),
    HOLD("HOLD", "보류");

    private final String code;
    private final String label;

    AdminOperationTaskStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static AdminOperationTaskStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown task status: " + code));
    }
}
