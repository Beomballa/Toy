package com.section.admin.product.res;

import com.section.common.commerce.dto.ProductDetailResDto;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.util.DateUtil;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProductDetailResponse {
    private Long productNo;
    private Long categoryNo;
    private String categoryName;
    private Long brandNo;
    private String brandName;
    private String productName;
    private String productModel;
    private Integer releasePrice;
    private LocalDate releaseDt;
    private String thumbnailUrl;
    private String crtDtm;
    private String uptDtm;

    List<OptionInfo> options;

    public ProductDetailResponse(ProductDetailResDto resDto, List<ProductOption> options) {
        this.productNo = resDto.getProductNo();
        this.categoryNo = resDto.getCategoryNo();
        this.categoryName = resDto.getCategoryName();
        this.brandNo = resDto.getBrandNo();
        this.brandName = resDto.getBrandName();
        this.productName = resDto.getProductName();
        this.productModel = resDto.getProductModel();
        this.releasePrice = resDto.getReleasePrice();
        this.releaseDt = resDto.getReleaseDt();
        this.thumbnailUrl = resDto.getThumbnailUrl();
        this.crtDtm = (resDto.getCrtDtm() != null ? DateUtil.localDateTimeToStr(resDto.getCrtDtm()) : "");
        this.uptDtm = (resDto.getUptDtm() != null ? DateUtil.localDateTimeToStr(resDto.getUptDtm()) : "");

        // 2. 엔티티 리스트를 내부 DTO 리스트로 변환 ⭐
        if (options != null) {
            this.options = options.stream()
                    .map(OptionInfo::new)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 내부 DTO: 화면 전달용 옵션 정보
     */
    @Getter
    @Setter
    public static class OptionInfo {
        private Long optionNo;
        private String optionName;
        private Integer stockQty;

        // 엔티티를 받아서 DTO로 변환하는 생성자
        public OptionInfo(ProductOption entity) {
            this.optionNo = entity.getId();
            this.optionName = entity.getOptionName();
            this.stockQty = entity.getStockCnt();
        }
    }
}
