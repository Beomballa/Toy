package com.section.admin.banner.support;

import com.section.common.commerce.dto.BannerListQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public record BannerExportSummary(
        String exportedAt,
        String sortLabel,
        String filterSummary
) {
    private static final DateTimeFormatter EXPORTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static BannerExportSummary from(BannerListQuery query) {
        List<String> filters = new ArrayList<>();
        if (query.keyword() != null) {
            filters.add("검색어: " + query.keyword());
        }
        if (query.isActive() != null) {
            filters.add("상태: " + ("Y".equalsIgnoreCase(query.isActive()) ? "사용" : "중지"));
        }
        if (query.exposureStatus() != null) {
            filters.add("노출상태: " + resolveExposureStatusLabel(query.exposureStatus()));
        }

        return new BannerExportSummary(
                LocalDateTime.now().format(EXPORTED_AT_FORMAT),
                "정렬 순서 오름차순 · 최근 등록 순",
                filters.isEmpty() ? "전체" : String.join(" | ", filters)
        );
    }

    private static String resolveExposureStatusLabel(String exposureStatus) {
        return switch (exposureStatus) {
            case "SCHEDULED" -> "대기";
            case "LIVE" -> "진행중";
            case "ENDED" -> "종료";
            default -> exposureStatus;
        };
    }
}
