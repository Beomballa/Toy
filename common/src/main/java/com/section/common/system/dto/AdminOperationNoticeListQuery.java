package com.section.common.system.dto;

import com.section.common.base.entity.type.AdminNoticeVisibilityStatus;

public record AdminOperationNoticeListQuery(
        String keyword,
        String isActive,
        String isPinned,
        AdminNoticeVisibilityStatus visibilityStatus
) {
    public AdminOperationNoticeListQuery toStatsQuery() {
        return new AdminOperationNoticeListQuery(keyword, isActive, null, null);
    }
}
