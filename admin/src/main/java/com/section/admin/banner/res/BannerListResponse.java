package com.section.admin.banner.res;

import com.section.common.commerce.dto.BannerListQuery;
import com.section.common.commerce.dto.BannerListResDto;

import java.time.LocalDateTime;
import java.util.List;

public record BannerListResponse(
        List<Item> items,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static BannerListResponse of(List<BannerListResDto> items, BannerListQuery query) {
        List<Item> mappedItems = items.stream().map(Item::from).toList();
        return new BannerListResponse(
                mappedItems,
                new AppliedQuery(query.keyword(), query.isActive()),
                ResultMeta.from(mappedItems.size(), query)
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
            String isActive
    ) {
    }

    public record ResultMeta(
            String resultLabel,
            long appliedFilterCount,
            boolean hasActiveFilters,
            String querySignature
    ) {
        private static ResultMeta from(int size, BannerListQuery query) {
            long filterCount = appliedFilterCount(query);
            boolean hasActiveFilters = filterCount > 0;
            return new ResultMeta(
                    hasActiveFilters ? String.format("검색 결과 %d건", size) : String.format("전체 %d건", size),
                    filterCount,
                    hasActiveFilters,
                    querySignature(query)
            );
        }

        private static long appliedFilterCount(BannerListQuery query) {
            long count = 0;
            if (query.keyword() != null) count++;
            if (query.isActive() != null) count++;
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
            return builder.toString();
        }
    }
}
