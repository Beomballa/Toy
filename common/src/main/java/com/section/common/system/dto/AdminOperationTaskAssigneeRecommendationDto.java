package com.section.common.system.dto;

public record AdminOperationTaskAssigneeRecommendationDto(
        Long adminNo,
        String adminName,
        long totalCount,
        long inProgressCount,
        long overdueCount
) {
}
