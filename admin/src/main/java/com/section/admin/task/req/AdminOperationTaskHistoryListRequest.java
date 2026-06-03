package com.section.admin.task.req;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class AdminOperationTaskHistoryListRequest {

    private static final Set<String> ALLOWED_ACTION_TYPES = Set.of(
            "TASK_",
            "TASK_CREATE",
            "TASK_UPDATE",
            "TASK_STATUS_UPDATE",
            "TASK_DUPLICATE",
            "TASK_BULK_UPDATE",
            "TASK_BULK_DUPLICATE",
            "TASK_COMMENT_CREATE",
            "TASK_COMMENT_UPDATE",
            "TASK_COMMENT_DELETE",
            "TASK_BULK_DELETE",
            "TASK_DELETE"
    );

    private Long taskNo;
    private String actionType;
    private Long adminNo;
    private LocalDate startDate;
    private LocalDate endDate;
    private String returnTo;

    public AdminLogListRequest toLogListRequest() {
        validate();

        AdminLogListRequest request = new AdminLogListRequest();
        request.setTargetId(taskNo);
        request.setAdminNo(adminNo);
        // TASK_ prefix means "all task logs" because admin log query uses containsIgnoreCase.
        request.setActionType(normalizedActionType());
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        return request;
    }

    public int normalizedPage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    public int normalizedSize(Integer size) {
        if (size == null || size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    public String normalizedReturnTo() {
        if (returnTo == null || returnTo.isBlank()) {
            return "/admin/settings/tasks";
        }
        return returnTo.trim();
    }

    private void validate() {
        if (taskNo != null && taskNo <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (adminNo != null && adminNo <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String normalizedActionType = normalizedActionType();
        if (normalizedActionType != null && !ALLOWED_ACTION_TYPES.contains(normalizedActionType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizedActionType() {
        if (actionType == null) {
            return "TASK_";
        }
        String normalized = actionType.trim().replaceAll("\\s+", " ").toUpperCase();
        return normalized.isBlank() ? "TASK_" : normalized;
    }
}
