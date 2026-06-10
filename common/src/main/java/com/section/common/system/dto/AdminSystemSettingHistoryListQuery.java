package com.section.common.system.dto;

import java.time.LocalDate;

public record AdminSystemSettingHistoryListQuery(
        String settingKey,
        Long adminNo,
        String adminKeyword,
        LocalDate startDate,
        LocalDate endDate
) {
}
