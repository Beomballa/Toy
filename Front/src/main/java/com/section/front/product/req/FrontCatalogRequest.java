package com.section.front.product.req;

import com.section.common.commerce.dto.FrontCatalogQuery;

public record FrontCatalogRequest(
        String keyword,
        String brand,
        String category,
        String stock,
        String sort,
        Integer lowStockThreshold,
        Boolean featuredOnly,
        String priceBand
) {

    public FrontCatalogQuery toQuery() {
        return new FrontCatalogQuery(
                normalizeText(keyword),
                normalizeText(brand),
                normalizeText(category),
                normalizeStock(stock),
                normalizeSort(sort),
                normalizeLowStockThreshold(lowStockThreshold),
                Boolean.TRUE.equals(featuredOnly),
                normalizePriceBand(priceBand)
        );
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank() || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private String normalizeStock(String value) {
        if (value == null || value.isBlank()) {
            return "ALL";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "LOW", "STABLE" -> normalized;
            default -> "ALL";
        };
    }

    private String normalizeSort(String value) {
        if (value == null || value.isBlank()) {
            return "LATEST";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "PRICE_HIGH", "PRICE_LOW", "STOCK_ASC", "STOCK_DESC", "FEATURED", "NAME_ASC" -> normalized;
            default -> "LATEST";
        };
    }

    private int normalizeLowStockThreshold(Integer value) {
        if (value == null) {
            return 20;
        }
        return switch (value) {
            case 10, 20, 30, 50 -> value;
            default -> 20;
        };
    }

    private String normalizePriceBand(String value) {
        if (value == null || value.isBlank()) {
            return "ALL";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "UNDER_200", "BETWEEN_200_300", "OVER_300" -> normalized;
            default -> "ALL";
        };
    }
}
