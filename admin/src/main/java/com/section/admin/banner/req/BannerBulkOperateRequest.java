package com.section.admin.banner.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

import java.util.List;

public record BannerBulkOperateRequest(
        List<Long> bannerNos,
        String isActive
) {
    public List<Long> normalizedBannerNos() {
        if (bannerNos == null || bannerNos.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<Long> normalized = bannerNos.stream()
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
        normalizedBannerNos();
        if (normalizedIsActive() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
