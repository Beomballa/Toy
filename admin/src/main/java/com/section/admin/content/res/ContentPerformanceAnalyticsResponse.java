package com.section.admin.content.res;

import java.util.List;

public record ContentPerformanceAnalyticsResponse(
        String boardType,
        int rangeDays,
        String startDate,
        String endDate,
        String generatedAt,
        Summary summary,
        List<Content> priorityContents
) {
    public record Summary(
            long totalViews,
            long totalReactions,
            int helpfulRate,
            int reactionCoverageRate,
            long analyzedContentCount,
            long actionRequiredCount,
            long linkedActionCount,
            long unlinkedActionCount,
            long openTaskCount,
            long overdueTaskCount,
            long recoverableTaskCount
    ) {
        public Summary(
                long totalViews,
                long totalReactions,
                int helpfulRate,
                int reactionCoverageRate,
                long analyzedContentCount,
                long actionRequiredCount,
                long linkedActionCount,
                long unlinkedActionCount
        ) {
            this(
                    totalViews, totalReactions, helpfulRate, reactionCoverageRate,
                    analyzedContentCount, actionRequiredCount, linkedActionCount, unlinkedActionCount,
                    0, 0, 0
            );
        }
    }

    public record Content(
            long documentId,
            String boardType,
            String title,
            long viewCount,
            long uniqueVisitors,
            long reactionCount,
            long helpfulCount,
            long notHelpfulCount,
            int helpfulRate,
            int reactionCoverageRate,
            int priorityScore,
            String status,
            String statusMessage,
            Long operationTaskNo,
            String operationTaskPath,
            String operationTaskStatus,
            String operationTaskStatusLabel,
            String operationTaskDueDate,
            boolean operationTaskOverdue,
            boolean operationTaskRecoverable
    ) {
        public Content(
                long documentId,
                String boardType,
                String title,
                long viewCount,
                long uniqueVisitors,
                long reactionCount,
                long helpfulCount,
                long notHelpfulCount,
                int helpfulRate,
                int reactionCoverageRate,
                int priorityScore,
                String status,
                String statusMessage,
                Long operationTaskNo,
                String operationTaskPath
        ) {
            this(
                    documentId, boardType, title, viewCount, uniqueVisitors, reactionCount,
                    helpfulCount, notHelpfulCount, helpfulRate, reactionCoverageRate, priorityScore,
                    status, statusMessage, operationTaskNo, operationTaskPath,
                    null, null, null, false, false
            );
        }
    }
}
