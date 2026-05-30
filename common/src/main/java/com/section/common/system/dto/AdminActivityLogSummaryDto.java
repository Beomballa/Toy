package com.section.common.system.dto;

public record AdminActivityLogSummaryDto(
        long totalCount,
        long todayCount,
        long noticeCount,
        long taskCount,
        long commerceCount,
        long adminCount
) {
}
