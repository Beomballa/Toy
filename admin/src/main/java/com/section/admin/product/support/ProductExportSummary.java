package com.section.admin.product.support;

import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.ProductListQuery;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record ProductExportSummary(
        String exportedAt,
        String orderTypeLabel,
        String filterSummary
) {
    public static ProductExportSummary from(ProductListQuery query, String brandName, String categoryName) {
        return new ProductExportSummary(
                ProductViewFormatter.formatExportedAt(LocalDateTime.now()),
                ProductViewFormatter.formatOrderType(query.orderType()),
                buildFilterSummary(query, brandName, categoryName)
        );
    }

    private static String buildFilterSummary(ProductListQuery query, String brandName, String categoryName) {
        List<String> chunks = new ArrayList<>();
        if (brandName != null && !brandName.isBlank()) {
            chunks.add("브랜드: " + brandName);
        }
        if (categoryName != null && !categoryName.isBlank()) {
            chunks.add("카테고리: " + categoryName);
        }
        if (query.status() != null) {
            chunks.add("상태: " + ProductStatus.fromCode(query.status().name()).getDesc());
        }
        if (query.lowStockOnly()) {
            chunks.add("저재고: " + query.effectiveLowStockThreshold() + "개 미만");
        }
        if (query.createdTodayOnly()) {
            chunks.add("오늘 등록만");
        }
        if (query.searchKeyword() != null) {
            chunks.add("검색어: " + query.searchKeyword());
        }

        return chunks.isEmpty() ? "추가 필터 없음" : String.join(" | ", chunks);
    }
}
