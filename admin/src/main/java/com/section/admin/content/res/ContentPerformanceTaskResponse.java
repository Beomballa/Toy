package com.section.admin.content.res;

public record ContentPerformanceTaskResponse(
        long taskNo,
        boolean created,
        String status,
        String priority,
        String dueDate,
        String taskPath,
        String message
) {
}
