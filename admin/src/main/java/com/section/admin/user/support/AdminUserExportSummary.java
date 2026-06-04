package com.section.admin.user.support;

import com.section.common.system.dto.AdminUserListQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AdminUserExportSummary(
        String exportedAt,
        String filterSummary
) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AdminUserExportSummary of(AdminUserListQuery query, LocalDateTime exportedAt) {
        return new AdminUserExportSummary(
                exportedAt.format(DATE_TIME_FORMATTER),
                buildFilterSummary(query)
        );
    }

    private static String buildFilterSummary(AdminUserListQuery query) {
        StringBuilder builder = new StringBuilder("권한 우선 · 최근 로그인순");
        if (query.keyword() != null) {
            builder.append(" · 검색=").append(query.keyword());
        }
        if (query.role() != null) {
            builder.append(" · 권한=").append("ROLE_SUPER".equals(query.role()) ? "최고 관리자" : "일반 관리자");
        }
        if (query.status() != null) {
            builder.append(" · 상태=").append("SUSPENDED".equals(query.status()) ? "정지" : "활성");
        }
        if (query.inactiveDays() != null) {
            builder.append(" · 미접속 ").append(query.inactiveDays()).append("일+");
        }
        if (Boolean.TRUE.equals(query.neverLoggedInOnly())) {
            builder.append(" · 로그인 이력 없음");
        }
        return builder.toString();
    }
}
