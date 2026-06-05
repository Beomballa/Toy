package com.section.admin.product.req;

import com.section.common.base.entity.type.ProductStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

import java.util.List;

public record ProductBulkOperateRequest(
        List<Long> productNos,
        String status
) {
    public List<Long> normalizedProductNos() {
        if (productNos == null || productNos.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Long> normalized = productNos.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (normalized.isEmpty() || normalized.stream().anyMatch(no -> no <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    public ProductStatus normalizedStatus() {
        if (status == null || status.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        try {
            ProductStatus normalized = ProductStatus.valueOf(status.trim().toUpperCase());
            if (ProductStatus.DELETE == normalized) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
