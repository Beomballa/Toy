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
        java.util.List<ReportDetail> reports,
        String createdAt
) {
    public record ReportDetail(String reason, String detail, String createdAt) {
    }
}
