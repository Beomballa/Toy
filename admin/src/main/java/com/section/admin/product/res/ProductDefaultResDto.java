package com.section.admin.product.res;

import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProductDefaultResDto {
    private List<BrandSimpleDto> brands;
    private List<CategorySimpleDto> categories;

    // ============================================================
    // 내부 DTO - 브랜드 간단 정보
    // ============================================================
    @Getter
    @Builder
    @AllArgsConstructor
    public static class BrandSimpleDto {
        private Long brandNo;
        private String nameKo;
        private String nameEn;

        // Entity -> DTO 변환
        public static BrandSimpleDto from(Brand brand) {
            return BrandSimpleDto.builder()
                    .brandNo(brand.getBrandNo())
                    .nameKo(brand.getNameKo())
                    .nameEn(brand.getNameEn())
                    .build();
        }
    }

    // ============================================================
    // 내부 DTO - 카테고리 간단 정보
    // ============================================================
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CategorySimpleDto {
        private Long categoryNo;
        private Long parentNo;
        private String name;
        private Integer depth;
        private String isActive;

        // Entity -> DTO 변환
        public static CategorySimpleDto from(Category category) {
            return CategorySimpleDto.builder()
                    .categoryNo(category.getCategoryNo())
                    .parentNo(category.getParentNo())
                    .name(category.getName())
                    .depth(category.getDepth())
                    .isActive(category.getIsActive())
                    .build();
        }
    }
}
