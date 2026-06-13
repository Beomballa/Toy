package com.section.admin.settings.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminSystemSettingHistoryListQuery;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class AdminSystemSettingHistoryListRequest {

    private static final Set<String> ALLOWED_SETTING_KEYS = Set.of(
            "SYSTEM_MAINTENANCE_MODE",
            "COMMUNITY_WRITE_ENABLED",
            "ORDER_EXPORT_ENABLED",
            "LOW_STOCK_DEFAULT_THRESHOLD"
    );

    private String settingKey;
    private Long adminNo;
    private String adminKeyword;
    private String changeStatus;
    private LocalDate startDate;
    private LocalDate endDate;

    public AdminSystemSettingHistoryListQuery toQuery() {
        validate();
        return new AdminSystemSettingHistoryListQuery(
                normalizedSettingKey(),
                adminNo,
                normalizedAdminKeyword(),
                normalizedChangeStatus(),
                startDate,
                endDate
        );
    }

    public int normalizedPage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    public int normalizedSize(Integer size) {
        if (size == null || size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private void validate() {
        if (adminNo != null && adminNo <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String normalizedSettingKey = normalizedSettingKey();
        if (normalizedSettingKey != null && !ALLOWED_SETTING_KEYS.contains(normalizedSettingKey)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (changeStatus != null && normalizedChangeStatus() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizedSettingKey() {
        if (settingKey == null || settingKey.isBlank()) {
            return null;
        }
        return settingKey.trim().toUpperCase();
    }

    private String normalizedAdminKeyword() {
        if (adminKeyword == null) {
            return null;
        }
        String normalized = adminKeyword.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizedChangeStatus() {
        if (changeStatus == null || changeStatus.isBlank()) {
            return null;
        }
        return switch (changeStatus.trim().toUpperCase()) {
            case "CURRENT", "OUTDATED" -> changeStatus.trim().toUpperCase();
            default -> null;
        };
    }
}
