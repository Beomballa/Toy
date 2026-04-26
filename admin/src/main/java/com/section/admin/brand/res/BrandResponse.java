package com.section.admin.brand.res;

import com.section.common.commerce.entity.Brand;

public record BrandResponse(
        Long brandNo,
        String nameKo,
        String nameEn,
        String logoUrl,
        String isActive
) {
    public static BrandResponse from(Brand brand) {
        return new BrandResponse(
                brand.getBrandNo(),
                brand.getNameKo(),
                brand.getNameEn(),
                brand.getLogoUrl(),
                brand.getIsActive()
        );
    }
}
