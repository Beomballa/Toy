package com.section.admin.task.req;

import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminOperationTaskListQuery;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AdminOperationTaskListRequest {

    private String keyword;
    private Long taskNo;
    private String status;
    private String priority;
    private Long assigneeAdminNo;
    private String isPinned;
    private String overdueOnly;
    private String unassignedOnly;
    private String commentedOnly;
    private Integer dueWithinDays;
    private String dueState;
    private String sortBy;
    private LocalDate dueDateFrom;
    private LocalDate dueDateTo;
    private Integer page = 0;
    private Integer size = 10;

    public AdminOperationTaskListQuery toQuery() {
        String normalizedUnassignedOnly = normalizeFlag(unassignedOnly);
        Long normalizedAssigneeAdminNo = normalizeAssigneeAdminNo(assigneeAdminNo);
        if ("Y".equalsIgnoreCase(normalizedUnassignedOnly)) {
            normalizedAssigneeAdminNo = null;
        }
        return new AdminOperationTaskListQuery(
                normalize(keyword),
                normalizeTaskNo(taskNo),
                normalizeStatus(status),
                normalizePriority(priority),
                normalizedAssigneeAdminNo,
                normalizeFlag(isPinned),
                normalizeFlag(overdueOnly),
                normalizedUnassignedOnly,
                normalizeFlag(commentedOnly),
                normalizeDueWithinDays(dueWithinDays),
                normalizeDueState(dueState),
                normalizeSortBy(sortBy),
                normalizeDueDateFrom(dueDateFrom, dueDateTo),
                normalizeDueDateTo(dueDateFrom, dueDateTo)
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

    private Long normalizeTaskNo(Long value) {
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

    private String normalizeSortBy(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return "PINNED_DUE";
        }
        return switch (normalized.toUpperCase()) {
            case "PINNED_DUE", "DUE_DATE_DESC", "PRIORITY_DESC", "CREATED_DESC", "LATEST_COMMENT_DESC", "COMMENT_COUNT_DESC" -> normalized.toUpperCase();
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        };
    }

    private Integer normalizeDueWithinDays(Integer value) {
        if (value == null) {
            return null;
        }
        if (value <= 0 || value > 30) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return value;
    }

    private LocalDate normalizeDueDateFrom(LocalDate from, LocalDate to) {
        validateDueDateRange(from, to);
        return from;
    }

    private LocalDate normalizeDueDateTo(LocalDate from, LocalDate to) {
        validateDueDateRange(from, to);
        return to;
    }

    private void validateDueDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizeDueState(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase()) {
            case "OVERDUE", "TODAY", "UPCOMING", "NO_DUE" -> normalized.toUpperCase();
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        };
    }
}
