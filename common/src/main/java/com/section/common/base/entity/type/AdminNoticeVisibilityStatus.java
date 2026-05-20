package com.section.common.base.entity.type;

public enum AdminNoticeVisibilityStatus {
    LIVE("노출중"),
    SCHEDULED("예약"),
    ENDED("종료"),
    INACTIVE("비활성");

    private final String label;

    AdminNoticeVisibilityStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static AdminNoticeVisibilityStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return AdminNoticeVisibilityStatus.valueOf(value.trim().toUpperCase());
    }
}
