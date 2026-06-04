package com.section.admin.category.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

import java.util.List;

public record CategoryBulkOperateRequest(
        List<Long> categoryNos,
        String isActive
) {
    public List<Long> normalizedCategoryNos() {
        if (categoryNos == null || categoryNos.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<Long> normalized = categoryNos.stream()
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
        normalizedCategoryNos();
        if (normalizedIsActive() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
