package com.section.admin.task.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

import java.util.List;

public record AdminOperationTaskBulkDuplicateRequest(
        List<Long> taskNos
) {
    public List<Long> normalizedTaskNos() {
        if (taskNos == null || taskNos.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<Long> normalized = taskNos.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (normalized.isEmpty() || normalized.stream().anyMatch(no -> no <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }
}
