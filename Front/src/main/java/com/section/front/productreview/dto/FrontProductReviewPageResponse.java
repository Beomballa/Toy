package com.section.front.productreview.dto;

import java.util.List;

public record FrontProductReviewPageResponse(
        List<FrontProductReviewResponse> reviews,
        long totalCount,
        double averageRating,
        int page,
        int totalPages,
        boolean hasNext
) {
}
