package com.section.common.commerce.dto;

import com.section.common.base.entity.type.ProductStatus;

public record AdminFrontDisplayProductQuery(
        String keyword,
        ProductStatus status,
        Long brandNo,
        Long categoryNo,
        Boolean displayConfigured,
        boolean featuredOnly,
        boolean lowStockOnly,
        long lowStockThreshold,
        String sort
) {
    public boolean configuredOnly() {
        return Boolean.TRUE.equals(displayConfigured);
    }

    public boolean unconfiguredOnly() {
        return Boolean.FALSE.equals(displayConfigured);
    }
}
