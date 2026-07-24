package com.section.admin.content.res;

import java.util.List;

public record ContentPerformanceBulkAssignResponse(
        int requestedCount,
        int assignedCount,
        int alreadyAssignedCount,
        int skippedCount,
        List<Assignment> assignments,
        String taskListPath,
        String message
) {
    public record Assignment(
            long taskNo,
            long adminNo,
            String adminName
    ) {
    }
}
