package com.section.admin.banner.res;

import com.section.common.commerce.entity.DisplayBanner;

import java.time.LocalDateTime;

public record BannerDetailResponse(
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
    public static BannerDetailResponse from(DisplayBanner banner) {
        return new BannerDetailResponse(
                banner.getBannerNo(),
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getTargetUrl(),
                format(banner.getStartDtm()),
                format(banner.getEndDtm()),
                banner.getSortOrder(),
                banner.getIsActive(),
                resolveDisplayStatus(banner)
        );
    }

    private static String format(LocalDateTime value) {
        return value == null ? "-" : value.toString().replace('T', ' ');
    }

    private static String resolveDisplayStatus(DisplayBanner banner) {
        if (!"Y".equalsIgnoreCase(banner.getIsActive())) {
            return "중지";
        }
        LocalDateTime now = LocalDateTime.now();
        if (banner.getStartDtm() != null && banner.getStartDtm().isAfter(now)) {
            return "대기";
        }
        if (banner.getEndDtm() != null && banner.getEndDtm().isBefore(now)) {
            return "종료";
        }
        return "노출중";
    }
}
