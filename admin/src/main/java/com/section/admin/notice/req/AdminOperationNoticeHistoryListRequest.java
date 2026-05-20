package com.section.admin.notice.req;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class AdminOperationNoticeHistoryListRequest {

    private static final Set<String> ALLOWED_ACTION_TYPES = Set.of(
            "NOTICE_",
            "NOTICE_CREATE",
            "NOTICE_UPDATE",
            "NOTICE_ACTIVE_UPDATE",
            "NOTICE_DELETE",
            "NOTICE_BULK_UPDATE"
    );

    private Long noticeNo;
    private String actionType;
    private Long adminNo;
    private LocalDate startDate;
    private LocalDate endDate;
    private String returnTo;

    public AdminLogListRequest toLogListRequest() {
        validate();

        AdminLogListRequest request = new AdminLogListRequest();
        request.setTargetId(noticeNo);
        request.setAdminNo(adminNo);
        // NOTICE_ prefix means "all notice logs" because admin log query uses containsIgnoreCase.
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
            return "/admin/settings/notices";
        }
        return returnTo.trim();
    }

    private void validate() {
        if (noticeNo != null && noticeNo <= 0) {
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
            return "NOTICE_";
        }
        String normalized = actionType.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? "NOTICE_" : normalized;
    }
}
