package com.section.admin.product.req;

import com.section.common.base.entity.type.ProductStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

public record ProductQuickOperateRequest(
        String status
) {
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
