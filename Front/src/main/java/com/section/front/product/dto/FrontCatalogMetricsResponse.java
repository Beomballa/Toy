package com.section.front.product.dto;

public record FrontCatalogMetricsResponse(
        int totalCount,
        int lowStockCount,
        String latestCreatedDate,
        int latestDropCount,
        int featuredCount,
        int totalStock,
        int averagePrice,
        int minimumPrice,
        int maximumPrice,
        int brandCount,
        int under200Count,
        int between200And300Count,
        int over300Count
) {
    public FrontCatalogMetricsResponse(
            int totalCount,
            int lowStockCount,
            String latestCreatedDate,
            int latestDropCount,
            int featuredCount,
            int totalStock
    ) {
        this(totalCount, lowStockCount, latestCreatedDate, latestDropCount, featuredCount, totalStock, 0, 0, 0, 0, 0, 0, 0);
    }
}
