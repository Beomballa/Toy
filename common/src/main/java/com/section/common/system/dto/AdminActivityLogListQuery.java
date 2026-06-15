package com.section.common.system.dto;

import java.time.LocalDate;

public record AdminActivityLogListQuery(
        Long adminNo,
        String adminKeyword,
        String actionType,
        Long targetId,
        LocalDate startDate,
        LocalDate endDate
) {
}
