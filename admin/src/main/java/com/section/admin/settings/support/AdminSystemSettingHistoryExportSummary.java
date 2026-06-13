package com.section.admin.settings.support;

import com.section.common.system.dto.AdminSystemSettingHistoryListQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AdminSystemSettingHistoryExportSummary(
        String exportedAt,
        String filterSummary
) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AdminSystemSettingHistoryExportSummary of(
            AdminSystemSettingHistoryListQuery query,
            LocalDateTime exportedAt
    ) {
        return new AdminSystemSettingHistoryExportSummary(
                exportedAt.format(DATE_TIME_FORMATTER),
                buildFilterSummary(query)
        );
    }

    private static String buildFilterSummary(AdminSystemSettingHistoryListQuery query) {
        StringBuilder builder = new StringBuilder("최신 변경순");
        if (query.settingKey() != null) {
            builder.append(" · 설정=").append(AdminSettingDefinition.fromKey(query.settingKey()).label());
        }
        if (query.adminNo() != null) {
            builder.append(" · 관리자=").append(query.adminNo());
        }
        if (query.adminKeyword() != null) {
            builder.append(" · 관리자검색=").append(query.adminKeyword());
        }
        if (query.changeStatus() != null) {
            builder.append(" · 이력상태=").append(resolveChangeStatusLabel(query.changeStatus()));
        }
        if (query.startDate() != null || query.endDate() != null) {
            builder.append(" · 기간=")
                    .append(query.startDate() == null ? "-" : query.startDate())
                    .append("~")
                    .append(query.endDate() == null ? "-" : query.endDate());
        }
        return builder.toString();
    }

    private static String resolveChangeStatusLabel(String changeStatus) {
        return switch (changeStatus.toUpperCase()) {
            case "CURRENT" -> "현재 반영중";
            case "OUTDATED" -> "과거 이력";
            default -> changeStatus;
        };
    }
}
