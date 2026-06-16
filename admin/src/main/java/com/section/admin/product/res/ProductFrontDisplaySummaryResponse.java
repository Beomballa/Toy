package com.section.admin.product.res;

import java.util.List;

public record ProductFrontDisplaySummaryResponse(
        int totalCount,
        int configuredCount,
        int unconfiguredCount,
        int readyContentCount,
        int incompleteContentCount,
        int featuredCount,
        int lowStockCount,
        long lowStockThreshold
) {
    public static ProductFrontDisplaySummaryResponse from(
            List<ProductFrontDisplayListResponse> items,
            long lowStockThreshold
    ) {
        int totalCount = items.size();
        int configuredCount = (int) items.stream().filter(ProductFrontDisplayListResponse::displayConfigured).count();
        int readyContentCount = (int) items.stream().filter(ProductFrontDisplayListResponse::contentReady).count();
        int featuredCount = (int) items.stream().filter(ProductFrontDisplayListResponse::featured).count();
        int lowStockCount = (int) items.stream()
                .filter(item -> item.totalStock() != null && item.totalStock() < lowStockThreshold)
                .count();
        return new ProductFrontDisplaySummaryResponse(
                totalCount,
                configuredCount,
                totalCount - configuredCount,
                readyContentCount,
                totalCount - readyContentCount,
                featuredCount,
                lowStockCount,
                lowStockThreshold
        );
    }
}
