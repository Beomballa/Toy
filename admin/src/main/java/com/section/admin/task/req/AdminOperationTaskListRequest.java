package com.section.admin.task.req;

import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminOperationTaskListQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOperationTaskListRequest {

    private String keyword;
    private String status;
    private String priority;
    private Long assigneeAdminNo;
    private String overdueOnly;
    private Integer page = 0;
    private Integer size = 10;

    public AdminOperationTaskListQuery toQuery() {
        return new AdminOperationTaskListQuery(
                normalize(keyword),
                normalizeStatus(status),
                normalizePriority(priority),
                normalizeAssigneeAdminNo(assigneeAdminNo),
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

    private String normalizeStatus(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return AdminOperationTaskStatus.fromCode(normalized).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
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

    private Long normalizeAssigneeAdminNo(Long value) {
        if (value == null || value == 0L) {
            return null;
        }
        if (value < 0L) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return value;
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
