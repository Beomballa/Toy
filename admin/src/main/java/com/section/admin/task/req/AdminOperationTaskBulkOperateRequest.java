package com.section.admin.task.req;

import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;

import java.time.LocalDate;
import java.util.List;

public record AdminOperationTaskBulkOperateRequest(
        List<Long> taskNos,
        String status,
        String priority,
        Long assigneeAdminNo,
        String assigneeMode,
        String isPinned,
        LocalDate dueDate,
        String dueDateMode
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

    public LocalDate normalizedDueDate() {
        return dueDate;
    }

    public String normalizedDueDateMode() {
        if (dueDateMode == null || dueDateMode.isBlank()) {
            return null;
        }
        String normalized = dueDateMode.trim().toUpperCase();
        if (!"CLEAR".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    public boolean hasDueDateChange() {
        return normalizedDueDate() != null || "CLEAR".equals(normalizedDueDateMode());
    }

    public void validateOperation() {
        normalizedTaskNos();
        if (normalizedStatus() == null
                && normalizedPriority() == null
                && !hasAssigneeChange()
                && normalizedIsPinned() == null
                && !hasDueDateChange()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
