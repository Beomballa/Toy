package com.section.admin.content.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ContentBulkDeleteRequest(
        List<Long> ids
) {
    public Set<Long> normalizedIds() {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null || id <= 0) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            normalized.add(id);
        }
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }
}
