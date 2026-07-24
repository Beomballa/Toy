package com.section.admin.content.res;

import java.util.List;

public record ContentPerformanceBulkTaskResponse(
        int requestedCount,
        int createdCount,
        int existingCount,
        int skippedCount,
        List<ContentPerformanceTaskResponse> tasks,
        String taskListPath,
        String message
) {
}
