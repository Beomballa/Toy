package com.section.front.productreview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FrontProductReviewCreateRequest(
        @NotBlank @Size(max = 50) String orderNumber,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(max = 1000) String content
) {
}
