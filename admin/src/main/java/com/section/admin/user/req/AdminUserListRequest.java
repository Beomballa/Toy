package com.section.admin.user.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminUserListQuery;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class AdminUserListRequest {

    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_ADMIN", "ROLE_SUPER");
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "SUSPENDED");

    private String keyword;
    private String role;
    private String status;
    private Integer inactiveDays;
    private String neverLoggedInOnly;

    public AdminUserListQuery toQuery() {
        Integer normalizedInactiveDays = inactiveDays;
        if (normalizedInactiveDays != null && normalizedInactiveDays <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return new AdminUserListQuery(
                normalize(keyword),
                normalizeEnum(role, ALLOWED_ROLES),
                normalizeEnum(status, ALLOWED_STATUSES),
                normalizedInactiveDays,
                normalizeFlag(neverLoggedInOnly)
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeEnum(String value, Set<String> allowedValues) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private Boolean normalizeFlag(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (!Set.of("Y", "N").contains(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return "Y".equals(normalized);
    }
}
