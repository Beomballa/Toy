package com.section.front.productreview.dto;

import java.util.List;

public record FrontMemberProductReviewPageResponse(
        List<FrontMemberProductReviewResponse> reviews,
        long totalCount,
        int page,
        int totalPages,
        boolean hasNext
) {
}
