package com.section.common.commerce.dto;

public record FrontCatalogQuery(
        String keyword,
        String brand,
        String category,
        String stock,
        String sort,
        int lowStockThreshold,
        boolean featuredOnly,
        String priceBand
) {
    public static FrontCatalogQuery defaultQuery() {
        return new FrontCatalogQuery(null, null, null, "ALL", "LATEST", 20, false, "ALL");
    }

    public boolean isLowStockOnly() {
        return "LOW".equalsIgnoreCase(stock);
    }

    public boolean isStableStockOnly() {
        return "STABLE".equalsIgnoreCase(stock);
    }

    public boolean isFeaturedSort() {
        return "FEATURED".equalsIgnoreCase(sort);
    }

    public boolean isUnder200Only() {
        return "UNDER_200".equalsIgnoreCase(priceBand);
    }

    public boolean isBetween200And300Only() {
        return "BETWEEN_200_300".equalsIgnoreCase(priceBand);
    }

    public boolean isOver300Only() {
        return "OVER_300".equalsIgnoreCase(priceBand);
    }
}
