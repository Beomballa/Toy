package com.section.admin.product.req;

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
public class ProductUpdateRequest {

    @NotNull(message = "상품 번호는 필수입니다.")
    private Long productNo;

    @NotNull(message = "카테고리를 선택해주세요")
    private Long categoryNo;

    @NotNull(message = "브랜드를 선택해주세요")
    private Long brandNo;

    @NotBlank(message = "상품명을 입력해주세요")
    @Size(max = 200, message = "상품명은 200자 이내로 입력해주세요")
    private String nameKo;

    @Size(max = 200, message = "모델 번호는 200자 이내로 입력해주세요")
    private String modelNum;

    @NotNull(message = "발매가를 입력해주세요")
    @Min(value = 0, message = "발매가는 0원 이상이어야 합니다.")
    private Integer releasePrice;

    private LocalDate releaseDt;

    @Size(max = 500, message = "썸네일 URL은 500자 이내로 입력해주세요")
    private String thumbnailUrl;

    private String status;

    private List<ProductOptionUpdateRequest> options;

    @Getter
    @Setter
    public static class ProductOptionUpdateRequest {
        @NotBlank(message = "옵션명을 입력해주세요")
        private String optionName;

        @NotNull(message = "수량을 입력해주세요")
        @Min(value = 0, message = "수량은 0개 이상이어야 합니다.")
        private Integer stockCnt;
    }
}
