package com.section.front.product.dto;

public record FrontCatalogMetricsResponse(
        int totalCount,
        int lowStockCount,
        String latestCreatedDate,
        int latestDropCount,
        int featuredCount,
        int totalStock
) {
}
