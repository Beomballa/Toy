package com.section.admin.notice.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminOperationNoticeListQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOperationNoticeListRequest {

    private String keyword;
    private String isActive;
    private String isPinned;
    private Integer page = 0;
    private Integer size = 10;

    public AdminOperationNoticeListQuery toQuery() {
        String normalizedActive = normalizeFlag(isActive);
        String normalizedPinned = normalizeFlag(isPinned);
        return new AdminOperationNoticeListQuery(
                normalize(keyword),
                normalizedActive,
                normalizedPinned
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
