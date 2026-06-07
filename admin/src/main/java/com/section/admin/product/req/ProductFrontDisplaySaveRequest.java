package com.section.admin.product.req;

import com.section.admin.product.support.ProductInputNormalizer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductFrontDisplaySaveRequest(
        @NotNull(message = "상품 번호는 필수입니다.")
        Long productNo,

        @NotBlank(message = "헤드라인을 입력해주세요.")
        @Size(max = 120, message = "헤드라인은 120자 이내로 입력해주세요.")
        String headline,

        @NotBlank(message = "설명 문구를 입력해주세요.")
        @Size(max = 1000, message = "설명 문구는 1000자 이내로 입력해주세요.")
        String description,

        @NotBlank(message = "무드 키워드를 입력해주세요.")
        @Size(max = 120, message = "무드 키워드는 120자 이내로 입력해주세요.")
        String mood,

        Boolean featured,

        @Min(value = 1, message = "노출 순서는 1 이상이어야 합니다.")
        @Max(value = 999, message = "노출 순서는 999 이하여야 합니다.")
        Integer featuredRank
) {
    public String normalizedHeadline() {
        return ProductInputNormalizer.normalizeRequiredText(headline);
    }

    public String normalizedDescription() {
        return ProductInputNormalizer.normalizeRequiredText(description);
    }

    public String normalizedMood() {
        return ProductInputNormalizer.normalizeRequiredText(mood);
    }

    public String normalizedFeaturedYn() {
        return Boolean.TRUE.equals(featured) ? "Y" : "N";
    }

    public Integer normalizedFeaturedRank() {
        if (!Boolean.TRUE.equals(featured)) {
            return 999;
        }
        return featuredRank == null ? 999 : featuredRank;
    }
}
