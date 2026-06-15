package com.section.common.commerce.dto;

public record BannerListQuery(
        String keyword,
        String isActive,
        String exposureStatus
) {
    public BannerStatsQuery toStatsQuery() {
        return new BannerStatsQuery(keyword, isActive);
    }
}
