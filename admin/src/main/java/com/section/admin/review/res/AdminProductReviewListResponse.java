package com.section.admin.review.res;

import java.util.List;

public record AdminProductReviewListResponse(
        List<AdminProductReviewResponse> reviews,
        long totalCount,
        int currentPage,
        int totalPages,
        boolean hasNext
) {
}
