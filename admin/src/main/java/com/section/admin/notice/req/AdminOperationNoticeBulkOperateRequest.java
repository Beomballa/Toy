package com.section.admin.notice.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

import java.util.List;

public record AdminOperationNoticeBulkOperateRequest(
        List<Long> noticeNos,
        String isActive,
        String isPinned
) {
    public List<Long> normalizedNoticeNos() {
        if (noticeNos == null || noticeNos.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<Long> normalized = noticeNos.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (normalized.isEmpty() || normalized.stream().anyMatch(no -> no <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    public String normalizedIsActive() {
        return normalizeFlag(isActive);
    }

    public String normalizedIsPinned() {
        return normalizeFlag(isPinned);
    }

    public void validateOperation() {
        normalizedNoticeNos();
        if (normalizedIsActive() == null && normalizedIsPinned() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizeFlag(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }
}
