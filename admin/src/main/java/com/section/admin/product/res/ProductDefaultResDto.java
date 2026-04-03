package com.section.admin.product.res;

import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;

import java.util.List;

public record ProductDefaultResDto(
        List<BrandSimpleDto> brands,
        List<CategorySimpleDto> categories
) {

    // ============================================================
    // 내부 record - 브랜드 간단 정보
    // ============================================================
    public record BrandSimpleDto(
            Long brandNo,
            String nameKo,
            String nameEn
    ) {
        public static BrandSimpleDto from(Brand brand) {
            return new BrandSimpleDto(
                    brand.getBrandNo(),
                    brand.getNameKo(),
                    brand.getNameEn()
            );
        }
    }

    // ============================================================
    // 내부 record - 카테고리 간단 정보
    // ============================================================
    public record CategorySimpleDto(
            Long categoryNo,
            Long parentNo,
            String name,
            Integer depth,
            String isActive
    ) {
        public static CategorySimpleDto from(Category category) {
            return new CategorySimpleDto(
                    category.getCategoryNo(),
                    category.getParentNo(),
                    category.getName(),
                    category.getDepth(),
                    category.getIsActive()
            );
        }
    }
}
