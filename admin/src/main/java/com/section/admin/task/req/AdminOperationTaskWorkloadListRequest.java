package com.section.admin.task.req;

import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminOperationTaskWorkloadListQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOperationTaskWorkloadListRequest {

    private String keyword;
    private String priority;
    private String overdueOnly;
    private Integer page = 0;
    private Integer size = 10;

    public AdminOperationTaskWorkloadListQuery toQuery() {
        return new AdminOperationTaskWorkloadListQuery(
                normalize(keyword),
                normalizePriority(priority),
                normalizeFlag(overdueOnly)
        );
    }

    public int normalizedPage() {
        return page == null || page < 0 ? 0 : page;
    }

    public int normalizedSize() {
        if (size == null || size <= 0) {
            return 10;
        }
        if (size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return size;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizePriority(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return AdminOperationTaskPriority.fromCode(normalized).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizeFlag(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (!"Y".equalsIgnoreCase(normalized) && !"N".equalsIgnoreCase(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized.toUpperCase();
    }
}
