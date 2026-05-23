package com.section.admin.task.req;

import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

import java.util.List;

public record AdminOperationTaskBulkOperateRequest(
        List<Long> taskNos,
        String status,
        String priority,
        Long assigneeAdminNo,
        String isPinned
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

    public void validateOperation() {
        normalizedTaskNos();
        if (normalizedStatus() == null
                && normalizedPriority() == null
                && normalizedAssigneeAdminNo() == null
                && normalizedIsPinned() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
