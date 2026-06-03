package com.section.admin.notice.support;

import com.section.common.base.entity.type.AdminNoticeVisibilityStatus;
import com.section.common.system.dto.AdminOperationNoticeListQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public record AdminOperationNoticeExportSummary(
        String exportedAt,
        String sortLabel,
        String filterSummary
) {
    private static final DateTimeFormatter EXPORTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static AdminOperationNoticeExportSummary from(AdminOperationNoticeListQuery query) {
        List<String> filters = new ArrayList<>();
        if (query.keyword() != null) {
            filters.add("검색어: " + query.keyword());
        }
        if (query.isActive() != null) {
            filters.add("상태: " + ("Y".equalsIgnoreCase(query.isActive()) ? "활성" : "비활성"));
        }
        if (query.isPinned() != null) {
            filters.add("고정: " + ("Y".equalsIgnoreCase(query.isPinned()) ? "고정" : "일반"));
        }
        if (query.visibilityStatus() != null) {
            filters.add("노출 상태: " + formatVisibilityStatus(query.visibilityStatus()));
        }
        return new AdminOperationNoticeExportSummary(
                LocalDateTime.now().format(EXPORTED_AT_FORMAT),
                "고정 우선 · 최신 등록 순",
                filters.isEmpty() ? "전체" : String.join(" | ", filters)
        );
    }

    private static String formatVisibilityStatus(AdminNoticeVisibilityStatus status) {
        return switch (status) {
            case LIVE -> "노출중";
            case SCHEDULED -> "예약";
            case ENDED -> "종료";
            case INACTIVE -> "비활성";
        };
    }
}
