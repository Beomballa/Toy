package com.section.admin.brand.support;

import com.section.admin.brand.req.BrandListRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record BrandExportSummary(
        String exportedAt,
        String filterSummary
) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static BrandExportSummary of(BrandListRequest request, LocalDateTime exportedAt) {
        return new BrandExportSummary(
                exportedAt.format(DATE_TIME_FORMATTER),
                buildFilterSummary(request)
        );
    }

    private static String buildFilterSummary(BrandListRequest request) {
        StringBuilder builder = new StringBuilder("브랜드명 기준");
        if (request.normalizedKeyword() != null) {
            builder.append(" · 검색=").append(request.normalizedKeyword());
        }
        if (request.normalizedIsActive() != null) {
            builder.append(" · 상태=").append("Y".equalsIgnoreCase(request.normalizedIsActive()) ? "사용" : "중지");
        }
        return builder.toString();
    }
}
