package com.section.admin.brand.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

import java.util.List;

public record BrandBulkOperateRequest(
        List<Long> brandNos,
        String isActive
) {
    public List<Long> normalizedBrandNos() {
        if (brandNos == null || brandNos.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<Long> normalized = brandNos.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (normalized.isEmpty() || normalized.stream().anyMatch(no -> no <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    public String normalizedIsActive() {
        if (isActive == null || isActive.isBlank()) {
            return null;
        }
        String normalized = isActive.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    public void validateOperation() {
        normalizedBrandNos();
        if (normalizedIsActive() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
