package com.section.admin.product.req;

import com.section.common.base.entity.type.ProductHistoryActionType;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.ProductHistoryListQuery;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProductHistoryListRequest {

    private Long productNo;
    private String actionType;
    private String keyword;
    private LocalDate startDate;
    private LocalDate endDate;

    public ProductHistoryListQuery toQuery() {
        if (productNo != null && productNo <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new ProductHistoryListQuery(
                productNo,
                parseActionType(actionType),
                normalizeKeyword(keyword),
                startDate,
                endDate
        );
    }

    private ProductHistoryActionType parseActionType(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return null;
        }
        try {
            return ProductHistoryActionType.valueOf(actionType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
