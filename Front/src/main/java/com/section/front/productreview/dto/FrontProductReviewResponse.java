package com.section.front.productreview.dto;

import com.section.common.commerce.entity.FrontProductReview;

import java.time.format.DateTimeFormatter;

public record FrontProductReviewResponse(
        long id,
        String reviewerName,
        int rating,
        String content,
        String createdDate
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public static FrontProductReviewResponse from(FrontProductReview review) {
        return new FrontProductReviewResponse(
                review.getId(),
                review.getReviewerName(),
                review.getRating(),
                review.getContent(),
                review.getCrtDtm().toLocalDate().format(DATE_FORMATTER)
        );
    }
}
