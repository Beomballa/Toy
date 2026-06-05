package com.section.admin.category.support;

import com.section.admin.category.req.CategoryListRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record CategoryExportSummary(
        String exportedAt,
        String filterSummary
) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static CategoryExportSummary of(CategoryListRequest request, LocalDateTime exportedAt) {
        return new CategoryExportSummary(
                exportedAt.format(DATE_TIME_FORMATTER),
                buildFilterSummary(request)
        );
    }

    private static String buildFilterSummary(CategoryListRequest request) {
        StringBuilder builder = new StringBuilder(request.normalizedDepth() == 2 ? "중분류 기준" : "대분류 기준");
        if (request.normalizedKeyword() != null) {
            builder.append(" · 검색=").append(request.normalizedKeyword());
        }
        if (request.normalizedIsActive() != null) {
            builder.append(" · 상태=").append("Y".equalsIgnoreCase(request.normalizedIsActive()) ? "사용" : "중지");
        }
        return builder.toString();
    }
}
