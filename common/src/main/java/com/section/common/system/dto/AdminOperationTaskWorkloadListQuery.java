package com.section.common.system.dto;

public record AdminOperationTaskWorkloadListQuery(
        String keyword,
        String priority,
        String overdueOnly
) {
}
