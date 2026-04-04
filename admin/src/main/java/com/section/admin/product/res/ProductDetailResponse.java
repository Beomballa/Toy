package com.section.admin.product.res;

import com.section.common.commerce.dto.ProductDetailResDto;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.util.DateUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record ProductDetailResponse(
        Long productNo,
        Long categoryNo,
        String categoryName,
        Long brandNo,
        String brandName,
        String productName,
        String productModel,
        Integer releasePrice,
        LocalDate releaseDt,
        String thumbnailUrl,
        String crtDtm,
        String uptDtm,
        List<OptionInfo> options
) {

    public static ProductDetailResponse from(ProductDetailResDto resDto, List<ProductOption> options) {
        return new ProductDetailResponse(
                resDto.getProductNo(),
                resDto.getCategoryNo(),
                resDto.getCategoryName(),
                resDto.getBrandNo(),
                resDto.getBrandName(),
                resDto.getProductName(),
                resDto.getProductModel(),
                resDto.getReleasePrice(),
                resDto.getReleaseDt(),
                resDto.getThumbnailUrl(),
                resDto.getCrtDtm() != null ? DateUtil.localDateTimeToStr(resDto.getCrtDtm()) : "",
                resDto.getUptDtm() != null ? DateUtil.localDateTimeToStr(resDto.getUptDtm()) : "",
                Optional.ofNullable(options)
                        .map(list -> list.stream().map(OptionInfo::from).collect(Collectors.toList()))
                        .orElse(List.of())
        );
    }

    /**
     * 내부 record: 화면 전달용 옵션 정보
     */
    public record OptionInfo(
            Long optionNo,
            String optionName,
            Integer stockQty
    ) {
        public static OptionInfo from(ProductOption entity) {
            return new OptionInfo(
                    entity.getId(),
                    entity.getOptionName(),
                    entity.getStockCnt()
            );
        }
    }
}
