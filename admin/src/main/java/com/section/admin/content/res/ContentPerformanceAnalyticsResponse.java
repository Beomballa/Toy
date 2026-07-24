package com.section.admin.content.res;

import java.util.List;

public record ContentPerformanceAnalyticsResponse(
        String boardType,
        int rangeDays,
        String startDate,
        String endDate,
        String generatedAt,
        Summary summary,
        List<Content> priorityContents,
        List<AssignmentRecommendation> assignmentRecommendations
) {
    public ContentPerformanceAnalyticsResponse(
            String boardType,
            int rangeDays,
            String startDate,
            String endDate,
            String generatedAt,
            Summary summary,
            List<Content> priorityContents
    ) {
        this(boardType, rangeDays, startDate, endDate, generatedAt, summary, priorityContents, List.of());
    }

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
            long recoverableTaskCount,
            long unassignedTaskCount
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
                    0, 0, 0, 0
            );
        }

        public Summary(
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
            this(
                    totalViews, totalReactions, helpfulRate, reactionCoverageRate,
                    analyzedContentCount, actionRequiredCount, linkedActionCount, unlinkedActionCount,
                    openTaskCount, overdueTaskCount, recoverableTaskCount, 0
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
            boolean operationTaskRecoverable,
            Long operationTaskAssigneeAdminNo
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
                    null, null, null, false, false, null
            );
        }

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
                String operationTaskPath,
                String operationTaskStatus,
                String operationTaskStatusLabel,
                String operationTaskDueDate,
                boolean operationTaskOverdue,
                boolean operationTaskRecoverable
        ) {
            this(
                    documentId, boardType, title, viewCount, uniqueVisitors, reactionCount,
                    helpfulCount, notHelpfulCount, helpfulRate, reactionCoverageRate, priorityScore,
                    status, statusMessage, operationTaskNo, operationTaskPath, operationTaskStatus,
                    operationTaskStatusLabel, operationTaskDueDate, operationTaskOverdue,
                    operationTaskRecoverable, null
            );
        }
    }

    public record AssignmentRecommendation(
            Long adminNo,
            String adminName,
            long totalCount,
            long inProgressCount,
            long overdueCount,
            String reasonLabel
    ) {
    }
}
