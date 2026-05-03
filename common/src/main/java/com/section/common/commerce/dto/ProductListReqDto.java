package com.section.common.commerce.dto;

import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductListReqDto {
    private Long categoryNo;
    private Long brandNo;
    private String status;
    private String searchKeyword;
    private String orderType;
    private Boolean lowStockOnly;
    private Long lowStockThreshold;
    private Boolean createdTodayOnly;

    public ProductListQuery toQuery() {
        Long normalizedCategoryNo = normalizeFilterId(categoryNo);
        Long normalizedBrandNo = normalizeFilterId(brandNo);
        ProductStatus normalizedStatus = normalizeStatus(status);
        String normalizedKeyword = normalizeKeyword(searchKeyword);
        ProductOrderType normalizedOrderType = normalizeOrderType(orderType);
        boolean normalizedLowStockOnly = Boolean.TRUE.equals(lowStockOnly);
        Long normalizedLowStockThreshold = normalizeLowStockThreshold(lowStockThreshold, normalizedLowStockOnly);
        boolean normalizedCreatedTodayOnly = Boolean.TRUE.equals(createdTodayOnly);

        return new ProductListQuery(
                normalizedCategoryNo,
                normalizedBrandNo,
                normalizedStatus,
                normalizedKeyword,
                normalizedOrderType,
                normalizedLowStockOnly,
                normalizedLowStockThreshold,
                normalizedCreatedTodayOnly
        );
    }

    private Long normalizeFilterId(Long value) {
        if (value == null || value == 0L) {
            return null;
        }
        if (value < 0L) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return value;
    }

    private ProductStatus normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String normalizedKeyword = keyword.trim().replaceAll("\\s+", " ");
        if (normalizedKeyword.length() > 50) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return normalizedKeyword;
    }

    private ProductOrderType normalizeOrderType(String orderType) {
        ProductOrderType normalizedOrderType = ProductOrderType.fromCode(orderType);
        if (normalizedOrderType == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return normalizedOrderType;
    }

    private Long normalizeLowStockThreshold(Long threshold, boolean lowStockOnly) {
        if (!lowStockOnly) {
            return null;
        }
        if (threshold == null) {
            return 100L;
        }
        if (threshold <= 0L) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return threshold;
    }
}
