package com.section.common.commerce.dto;

import com.section.common.base.entity.type.ProductStatus;

public record AdminFrontDisplayProductQuery(
        String keyword,
        ProductStatus status,
        Long brandNo,
        Long categoryNo,
        Boolean displayConfigured,
        String contentStatus,
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

    public boolean readyContentOnly() {
        return "READY".equals(contentStatus);
    }

    public boolean incompleteContentOnly() {
        return "INCOMPLETE".equals(contentStatus);
    }
}
