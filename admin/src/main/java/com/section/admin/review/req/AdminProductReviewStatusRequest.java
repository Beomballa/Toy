package com.section.admin.review.req;

import com.section.common.commerce.entity.FrontProductReviewStatus;
import jakarta.validation.constraints.NotNull;

public record AdminProductReviewStatusRequest(@NotNull FrontProductReviewStatus status) {
}
