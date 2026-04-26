package com.section.admin.brand.req;

public record BrandSaveRequest(
        Long brandNo,
        String nameKo,
        String nameEn,
        String logoUrl,
        String isActive
) {}
