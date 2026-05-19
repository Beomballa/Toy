package com.section.common.system.dto;

public record AdminOperationNoticeListQuery(
        String keyword,
        String isActive,
        String isPinned
) {
}
