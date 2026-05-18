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

    /**
     * 상단 통계 카드는 빠른 필터(status/저재고/오늘등록)에 잠식되지 않고,
     * 기본 탐색 문맥(브랜드/카테고리/검색어)만 공유해야 의미가 유지됩니다.
     */
    public ProductListQuery toStatsQuery() {
        return new ProductListQuery(
                categoryNo,
                brandNo,
                null,
                searchKeyword,
                orderType,
                false,
                lowStockThreshold,
                false
        );
    }
}
