package com.section.admin.product.res;

import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.AdminFrontDisplayProductRow;

public record ProductFrontDisplayListResponse(
        Long productNo,
        String productName,
        String brandName,
        String categoryName,
        Integer releasePrice,
        Long totalStock,
        String status,
        String statusDescription,
        boolean displayConfigured,
        String headline,
        String description,
        String mood,
        boolean featured,
        Integer featuredRank
) {
    public static ProductFrontDisplayListResponse from(AdminFrontDisplayProductRow row) {
        return new ProductFrontDisplayListResponse(
                row.productNo(),
                row.productName(),
                row.brandName(),
                row.categoryName(),
                row.releasePrice(),
                row.totalStock(),
                row.status(),
                ProductStatus.fromCode(row.status()).getDesc(),
                row.displayConfigured(),
                row.headline(),
                row.description(),
                row.mood(),
                row.featured(),
                row.featuredRank() == null ? 999 : row.featuredRank()
        );
    }
}
