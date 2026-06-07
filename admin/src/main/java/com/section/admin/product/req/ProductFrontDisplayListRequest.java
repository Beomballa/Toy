package com.section.admin.product.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.base.entity.type.ProductStatus;

public record ProductFrontDisplayListRequest(
        String keyword,
        String status,
        Long brandNo,
        Long categoryNo,
        String configured,
        Boolean featuredOnly,
        Boolean lowStockOnly,
        Integer lowStockThreshold,
        String sort
) {
    public String normalizedKeyword() {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isBlank() ? null : normalized;
    }

    public ProductStatus normalizedStatus() {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public Long normalizedBrandNo() {
        return normalizePositiveFilterId(brandNo);
    }

    public Long normalizedCategoryNo() {
        return normalizePositiveFilterId(categoryNo);
    }

    public Boolean normalizedConfigured() {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        return switch (configured.trim().toUpperCase()) {
            case "ALL" -> null;
            case "CONFIGURED" -> true;
            case "UNCONFIGURED" -> false;
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        };
    }

    public boolean normalizedFeaturedOnly() {
        return Boolean.TRUE.equals(featuredOnly);
    }

    public boolean normalizedLowStockOnly() {
        return Boolean.TRUE.equals(lowStockOnly);
    }

    public long normalizedLowStockThreshold(long defaultThreshold) {
        if (lowStockThreshold == null) {
            return defaultThreshold;
        }
        if (lowStockThreshold < 1) {
            return defaultThreshold;
        }
        return lowStockThreshold;
    }

    public String normalizedSort() {
        if (sort == null || sort.isBlank()) {
            return "FEATURED";
        }
        return switch (sort.trim().toUpperCase()) {
            case "FEATURED", "LATEST", "STOCK_ASC", "STOCK_DESC", "PRICE_HIGH", "PRICE_LOW" -> sort.trim().toUpperCase();
            default -> "FEATURED";
        };
    }

    private Long normalizePositiveFilterId(Long value) {
        if (value == null || value == 0L) {
            return null;
        }
        if (value < 0L) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return value;
    }
}
