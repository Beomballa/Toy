package com.section.admin.notice.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

import java.util.List;

public record AdminOperationNoticeBulkDeleteRequest(
        List<Long> noticeNos
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
}
