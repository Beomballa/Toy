package com.section.admin.product.req;

public record BrandSaveRequest(
        Long brandNo,
        String nameKo,
        String nameEn,
        String logoUrl,
        String isActive
) {}
