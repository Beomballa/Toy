package com.section.admin.settings.res;

import com.section.admin.settings.support.AdminSettingDefinition;
import com.section.common.system.entity.AdminSystemSettingHistory;

import java.time.format.DateTimeFormatter;

public record AdminSystemSettingHistoryDetailResponse(
        Long historyNo,
        String settingKey,
        String settingName,
        String beforeValue,
        String afterValue,
        String beforeValueLabel,
        String afterValueLabel,
        String currentValue,
        String currentValueLabel,
        boolean currentValueMatched,
        String changeSummary,
        Long changedAdminNo,
        String changedAdminName,
        String changedIpAddress,
        String changedAt
) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static AdminSystemSettingHistoryDetailResponse from(
            AdminSystemSettingHistory history,
            String adminName,
            String currentValue,
            boolean currentValueMatched
    ) {
        AdminSettingDefinition definition = AdminSettingDefinition.fromKey(history.getSettingKey());
        Long changedAdminNo = history.getCrtNo();
        return new AdminSystemSettingHistoryDetailResponse(
                history.getHistoryNo(),
                history.getSettingKey(),
                history.getSettingName(),
                history.getBeforeValue(),
                history.getAfterValue(),
                definition.formatValue(history.getBeforeValue()),
                definition.formatValue(history.getAfterValue()),
                definition.normalizeStoredValue(currentValue),
                definition.formatValue(currentValue),
                currentValueMatched,
                history.getChangeSummary(),
                changedAdminNo,
                changedAdminNo == null ? "관리자" : adminName,
                history.getChangedIpAddress(),
                history.getCrtDtm() == null ? "-" : history.getCrtDtm().format(DATE_TIME_FORMATTER)
        );
    }
}
