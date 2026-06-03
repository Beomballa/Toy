package com.section.admin.log.support;

import com.section.common.system.dto.AdminActivityLogListQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AdminLogExportSummary(
        String exportedAt,
        String filterSummary
) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AdminLogExportSummary of(AdminActivityLogListQuery query, LocalDateTime exportedAt) {
        return new AdminLogExportSummary(
                exportedAt.format(DATE_TIME_FORMATTER),
                buildFilterSummary(query)
        );
    }

    private static String buildFilterSummary(AdminActivityLogListQuery query) {
        StringBuilder builder = new StringBuilder("최신순");
        if (query.adminNo() != null) {
            builder.append(" · 관리자=").append(query.adminNo());
        }
        if (query.actionType() != null && !query.actionType().isBlank()) {
            builder.append(" · 작업=").append(query.actionType());
        }
        if (query.targetId() != null) {
            builder.append(" · 대상=").append(query.targetId());
        }
        if (query.startDate() != null || query.endDate() != null) {
            builder.append(" · 기간=")
                    .append(query.startDate() == null ? "-" : query.startDate())
                    .append("~")
                    .append(query.endDate() == null ? "-" : query.endDate());
        }
        return builder.toString();
    }
}
