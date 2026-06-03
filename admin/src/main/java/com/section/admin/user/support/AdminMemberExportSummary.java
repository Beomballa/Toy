package com.section.admin.user.support;

import com.section.common.system.dto.AccountListQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AdminMemberExportSummary(
        String exportedAt,
        String filterSummary
) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AdminMemberExportSummary of(AccountListQuery query, LocalDateTime exportedAt) {
        return new AdminMemberExportSummary(
                exportedAt.format(DATE_TIME_FORMATTER),
                buildFilterSummary(query)
        );
    }

    private static String buildFilterSummary(AccountListQuery query) {
        StringBuilder builder = new StringBuilder("최신 가입순");
        if (query.keyword() != null) {
            builder.append(" · 검색=").append(query.keyword());
        }
        if (query.masterYn() != null) {
            builder.append(" · 권한=").append(query.masterYn().name());
        }
        if (query.delYn() != null) {
            builder.append(" · 상태=").append(query.delYn().name());
        }
        return builder.toString();
    }
}
