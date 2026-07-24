package com.section.admin.content.res;

import java.util.List;

public record ContentPerformanceBulkResolveResponse(
        int requestedCount,
        int completedCount,
        int alreadyCompletedCount,
        int skippedCount,
        List<Long> completedTaskNos,
        String taskListPath,
        String message
) {
}
