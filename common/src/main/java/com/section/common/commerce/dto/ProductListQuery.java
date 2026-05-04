package com.section.common.commerce.dto;

import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.base.entity.type.ProductStatus;

public record ProductListQuery(
        Long categoryNo,
        Long brandNo,
        ProductStatus status,
        String searchKeyword,
        ProductOrderType orderType,
        boolean lowStockOnly,
        Long lowStockThreshold,
        boolean createdTodayOnly
) {
    public long effectiveLowStockThreshold() {
        return lowStockThreshold == null ? 100L : lowStockThreshold;
    }
}
