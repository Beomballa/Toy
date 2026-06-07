package com.section.admin.product.support;

import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.AdminFrontDisplayProductQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public record ProductFrontDisplayExportSummary(
        String exportedAt,
        String sortLabel,
        String filterSummary
) {
    private static final DateTimeFormatter EXPORTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static ProductFrontDisplayExportSummary of(
            AdminFrontDisplayProductQuery query,
            String brandName,
            String categoryName
    ) {
        return new ProductFrontDisplayExportSummary(
                LocalDateTime.now().format(EXPORTED_AT_FORMAT),
                sortLabel(query.sort()),
                buildFilterSummary(query, brandName, categoryName)
        );
    }

    private static String sortLabel(String sort) {
        if (sort == null) {
            return "Featured 우선";
        }
        return switch (sort) {
            case "LATEST" -> "최신 등록순";
            case "STOCK_ASC" -> "재고 낮은 순";
            case "STOCK_DESC" -> "재고 높은 순";
            case "PRICE_HIGH" -> "발매가 높은 순";
            case "PRICE_LOW" -> "발매가 낮은 순";
            default -> "Featured 우선";
        };
    }

    private static String buildFilterSummary(
            AdminFrontDisplayProductQuery query,
            String brandName,
            String categoryName
    ) {
        List<String> parts = new ArrayList<>();
        if (query.keyword() != null) {
            parts.add("검색어: " + query.keyword());
        }
        if (query.status() != null) {
            parts.add("상태: " + ProductStatus.fromCode(query.status().name()).getDesc());
        }
        if (brandName != null) {
            parts.add("브랜드: " + brandName);
        }
        if (categoryName != null) {
            parts.add("카테고리: " + categoryName);
        }
        if (query.configuredOnly()) {
            parts.add("노출 설정: 설정됨");
        }
        if (query.unconfiguredOnly()) {
            parts.add("노출 설정: 미설정");
        }
        if (query.featuredOnly()) {
            parts.add("Featured만");
        }
        if (query.lowStockOnly()) {
            parts.add("저재고: " + query.lowStockThreshold() + "개 미만");
        }
        return parts.isEmpty() ? "전체 상품" : String.join(" | ", parts);
    }
}
