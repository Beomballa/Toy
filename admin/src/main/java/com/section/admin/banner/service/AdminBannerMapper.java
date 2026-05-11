package com.section.admin.banner.service;

import com.section.admin.banner.req.BannerSaveRequest;
import com.section.common.commerce.entity.DisplayBanner;
import org.springframework.stereotype.Component;

@Component
public class AdminBannerMapper {

    public DisplayBanner toEntity(BannerSaveRequest req) {
        return DisplayBanner.builder()
                .bannerNo(req.bannerNo())
                .title(req.title().trim())
                .imageUrl(req.imageUrl().trim())
                .targetUrl(normalizeOptional(req.targetUrl()))
                .startDtm(req.startDtm())
                .endDtm(req.endDtm())
                .sortOrder(req.sortOrder())
                .isActive(req.isActive().trim().toUpperCase())
                .crtAdminNo(1L)
                .build();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
