package com.section.admin.banner.res;

import com.section.common.commerce.dto.BannerListQuery;
import com.section.common.commerce.dto.BannerListResDto;
import com.section.common.commerce.dto.BannerStatsQuery;
import com.section.common.commerce.dto.BannerSummaryDto;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public record BannerListResponse(
        List<Item> items,
        int currentPage,
        int totalPages,
        long totalElements,
        int pageSize,
        BannerStats bannerStats,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static BannerListResponse of(Page<BannerListResDto> page, BannerListQuery query, BannerSummaryDto summary) {
        List<Item> mappedItems = page.getContent().stream().map(Item::from).toList();
        return new BannerListResponse(
                mappedItems,
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                BannerStats.from(summary, query.toStatsQuery()),
                new AppliedQuery(query.keyword(), query.isActive(), query.exposureStatus()),
                ResultMeta.from(page, query)
        );
    }

    public record Item(
            Long bannerNo,
            String title,
            String imageUrl,
            String targetUrl,
            String startDtm,
            String endDtm,
            Integer sortOrder,
            String isActive,
            String displayStatus
    ) {
        public static Item from(BannerListResDto item) {
            return new Item(
                    item.getBannerNo(),
                    item.getTitle(),
                    item.getImageUrl(),
                    item.getTargetUrl(),
                    format(item.getStartDtm()),
                    format(item.getEndDtm()),
                    item.getSortOrder(),
                    item.getIsActive(),
                    resolveDisplayStatus(item)
            );
        }

        private static String format(LocalDateTime value) {
            return value == null ? "-" : value.toString().replace('T', ' ');
        }

        private static String resolveDisplayStatus(BannerListResDto item) {
            if (!"Y".equals(item.getIsActive())) {
                return "중지";
            }
            LocalDateTime now = LocalDateTime.now();
            if (item.getStartDtm() != null && item.getStartDtm().isAfter(now)) {
                return "대기";
            }
            if (item.getEndDtm() != null && item.getEndDtm().isBefore(now)) {
                return "종료";
            }
            return "노출중";
        }
    }

    public record AppliedQuery(
            String keyword,
            String isActive,
            String exposureStatus
    ) {
    }

    public record BannerStats(
            long totalCount,
            long liveCount,
            long scheduledCount,
            long endedCount,
            long inactiveCount,
            String contextLabel,
            String querySignature
    ) {
        static BannerStats from(BannerSummaryDto summary, BannerStatsQuery query) {
            return new BannerStats(
                    summary.totalCount(),
                    summary.liveCount(),
                    summary.scheduledCount(),
                    summary.endedCount(),
                    summary.inactiveCount(),
                    query.keyword() == null && query.isActive() == null ? "기본 문맥 기준" : "검색 문맥 기준",
                    buildQuerySignature(query)
            );
        }

        private static String buildQuerySignature(BannerStatsQuery query) {
            StringBuilder builder = new StringBuilder("정렬 순서 기준");
            if (query.keyword() != null) {
                builder.append(" · 검색=").append(query.keyword());
            }
            if (query.isActive() != null) {
                builder.append(" · 상태=").append("Y".equalsIgnoreCase(query.isActive()) ? "사용" : "중지");
            }
            return builder.toString();
        }
    }

    public record ResultMeta(
            String resultLabel,
            String pageInfoLabel,
            long appliedFilterCount,
            boolean hasActiveFilters,
            String querySignature,
            long rangeStart,
            long rangeEnd
    ) {
        private static ResultMeta from(Page<BannerListResDto> page, BannerListQuery query) {
            long filterCount = appliedFilterCount(query);
            boolean hasActiveFilters = filterCount > 0;
            long totalElements = page.getTotalElements();
            long rangeStart = totalElements == 0 ? 0 : (long) page.getNumber() * page.getSize() + 1;
            long rangeEnd = totalElements == 0 ? 0 : rangeStart + page.getNumberOfElements() - 1;
            String resultLabel = hasActiveFilters
                    ? String.format("검색 결과 %d건", totalElements)
                    : String.format("전체 %d건", totalElements);

            return new ResultMeta(
                    resultLabel,
                    totalElements == 0
                            ? "조건에 맞는 배너가 없습니다."
                            : String.format("%d-%d / %d건 · %d페이지", rangeStart, rangeEnd, totalElements, Math.max(page.getTotalPages(), 1)),
                    filterCount,
                    hasActiveFilters,
                    querySignature(query),
                    rangeStart,
                    rangeEnd
            );
        }

        private static long appliedFilterCount(BannerListQuery query) {
            long count = 0;
            if (query.keyword() != null) count++;
            if (query.isActive() != null) count++;
            if (query.exposureStatus() != null) count++;
            return count;
        }

        private static String querySignature(BannerListQuery query) {
            StringBuilder builder = new StringBuilder("정렬 순서 기준");
            if (query.keyword() != null) {
                builder.append(" · 검색=").append(query.keyword());
            }
            if (query.isActive() != null) {
                builder.append(" · 상태=").append("Y".equalsIgnoreCase(query.isActive()) ? "사용" : "중지");
            }
            if (query.exposureStatus() != null) {
                builder.append(" · 노출기간=").append(resolveExposureStatusLabel(query.exposureStatus()));
            }
            return builder.toString();
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
}
