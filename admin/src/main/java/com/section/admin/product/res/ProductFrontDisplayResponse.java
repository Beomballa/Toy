package com.section.admin.product.res;

import com.section.common.commerce.entity.FrontProductDisplay;

public record ProductFrontDisplayResponse(
        Long productNo,
        String headline,
        String description,
        String mood,
        boolean featured,
        Integer featuredRank
) {
    public static ProductFrontDisplayResponse from(Long productNo, FrontProductDisplay display) {
        if (display == null) {
            return new ProductFrontDisplayResponse(productNo, "", "", "", false, 999);
        }
        return new ProductFrontDisplayResponse(
                productNo,
                display.getHeadline(),
                display.getDescription(),
                display.getMood(),
                display.isFeatured(),
                display.getFeaturedRank()
        );
    }
}
