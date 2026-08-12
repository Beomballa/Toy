package com.section.front.productreview.dto;

public record FrontMemberProductReviewResponse(
        long id,
        long productId,
        String productName,
        String productBrand,
        String thumbnailUrl,
        int rating,
        String content,
        String createdDate
) {
}
