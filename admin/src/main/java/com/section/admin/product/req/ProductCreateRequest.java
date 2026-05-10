package com.section.admin.product.req;

import com.section.admin.product.support.ProductInputNormalizer;
import com.section.common.commerce.dto.ProductCreateReqDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ProductCreateRequest {
    @NotNull(message = "카테고리를 선택해주세요")
    private Long categoryNo;

    @NotNull(message = "브랜드를 선택해주세요")
    private Long brandNo;

    @NotBlank(message = "상품명을 입력해주세요")
    @Size(max = 200, message = "상품명은 200자 이내로 입력해주세요")
    private String nameKo;

    @Size(max = 200, message = "상품명은 200자 이내로 입력해주세요")
    private String modelNum;

    @NotNull(message = "발매가를 입력해주세요")
    @Min(value = 0, message = "발매가는 0원 이상이어야 합니다.")
    private Integer releasePrice;

    private LocalDate releaseDt;

    @Size(max = 500, message = "썸네일 URL은 500자 이내로 입력해주세요")
    private String thumbnailUrl;

    @Valid
    private List<ProductOptionRequest> options;

    public ProductCreateReqDto toProductCreateReqDto() {
        ProductCreateReqDto reqDto = new ProductCreateReqDto();
        reqDto.setCategoryNo(categoryNo);
        reqDto.setBrandNo(brandNo);
        reqDto.setNameKo(normalizeRequiredText(nameKo));
        reqDto.setModelNum(normalizeOptionalText(modelNum));
        reqDto.setReleasePrice(releasePrice);
        reqDto.setReleaseDt(releaseDt);
        reqDto.setThumbnailUrl(normalizeOptionalText(thumbnailUrl));
        return reqDto;
    }

    public String normalizeRequiredText(String value) {
        return ProductInputNormalizer.normalizeRequiredText(value);
    }

    public String normalizeOptionalText(String value) {
        return ProductInputNormalizer.normalizeOptionalText(value);
    }

    @Getter
    @Setter
    public static class ProductOptionRequest {
        @NotBlank(message = "옵션명을 입력해주세요")
        @Size(max = 100, message = "옵션명은 100자 이내로 입력해주세요")
        private String optionName;

        @NotNull(message = "수량을 입력해주세요")
        @Min(value = 0, message = "수량은 0개 이상이어야 합니다.")
        private Integer stockCnt;

        @NotNull(message = "추가 금액을 입력해주세요")
        @Min(value = 0, message = "추가 금액은 0원 이상이어야 합니다.")
        private Integer additionalPrice;

        public String normalizeOptionName() {
            return ProductInputNormalizer.normalizeRequiredText(optionName);
        }
    }
}
