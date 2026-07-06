package com.section.admin.task.req;

import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

public record AdminOperationTaskQuickOperateRequest(
        String status,
        String priority,
        Long assigneeAdminNo,
        String assigneeMode,
        String isPinned
) {
    public boolean hasOperateField() {
        return normalizedStatus() != null
                || normalizedPriority() != null
                || hasAssigneeChange()
                || normalizedIsPinned() != null;
    }

    public String normalizedStatus() {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return AdminOperationTaskStatus.fromCode(status).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public String normalizedPriority() {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        try {
            return AdminOperationTaskPriority.fromCode(priority).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public Long normalizedAssigneeAdminNo() {
        if (assigneeAdminNo == null || assigneeAdminNo == 0L) {
            return null;
        }
        if (assigneeAdminNo < 0L) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return assigneeAdminNo;
    }

    public String normalizedAssigneeMode() {
        if (assigneeMode == null || assigneeMode.isBlank()) {
            return null;
        }
        String normalized = assigneeMode.trim().toUpperCase();
        if (!"CLEAR".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    public boolean hasAssigneeChange() {
        return normalizedAssigneeAdminNo() != null || "CLEAR".equals(normalizedAssigneeMode());
    }

    public String normalizedIsPinned() {
        if (isPinned == null || isPinned.isBlank()) {
            return null;
        }
        String normalized = isPinned.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }
}
