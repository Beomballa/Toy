package com.section.admin.review.res;

public record AdminProductReviewResponse(
        long reviewId,
        long productId,
        String productName,
        String brandName,
        String reviewerName,
        int rating,
        String content,
        String status,
        String statusLabel,
        long reportCount,
        long pendingReportCount,
        java.util.List<ReportDetail> reports,
        java.util.List<StatusHistoryDetail> statusHistories,
        String createdAt
) {
    public record ReportDetail(String reason, String detail, String statusLabel, String createdAt) {
    }

    public record StatusHistoryDetail(String actionLabel, String beforeStatusLabel, String afterStatusLabel, String actorName, String createdAt) {
    }
}
