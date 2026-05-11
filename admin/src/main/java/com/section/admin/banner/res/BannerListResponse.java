package com.section.admin.banner.res;

import com.section.common.commerce.dto.BannerListQuery;
import com.section.common.commerce.dto.BannerListResDto;

import java.time.LocalDateTime;
import java.util.List;

public record BannerListResponse(
        List<Item> items,
        AppliedQuery appliedQuery
) {
    public static BannerListResponse of(List<BannerListResDto> items, BannerListQuery query) {
        return new BannerListResponse(items.stream().map(Item::from).toList(), new AppliedQuery(query.keyword(), query.isActive()));
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
}
