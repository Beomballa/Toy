package com.section.admin.product.res;

import com.section.common.commerce.dto.ProductDetailResDto;
import com.section.common.base.entity.type.ProductStatus;
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
        boolean hasThumbnail,
        String statusCode,
        String statusDesc,
        String crtDtm,
        String uptDtm,
        int optionCount,
        long totalStock,
        List<OptionInfo> options
) {

    public static ProductDetailResponse from(ProductDetailResDto resDto, List<ProductOption> options) {
        ProductStatus status = ProductStatus.fromCode(resDto.getStatus());
        List<OptionInfo> optionInfos = Optional.ofNullable(options)
                .map(list -> list.stream().map(OptionInfo::from).collect(Collectors.toList()))
                .orElse(List.of());

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
                resDto.getThumbnailUrl() != null && !resDto.getThumbnailUrl().isBlank(),
                status.name(),
                status.getDesc(),
                resDto.getCrtDtm() != null ? DateUtil.localDateTimeToStr(resDto.getCrtDtm()) : "",
                resDto.getUptDtm() != null ? DateUtil.localDateTimeToStr(resDto.getUptDtm()) : "",
                optionInfos.size(),
                optionInfos.stream()
                        .mapToLong(option -> option.stockQty() == null ? 0 : option.stockQty())
                        .sum(),
                optionInfos
        );
    }

    /**
     * 내부 record: 화면 전달용 옵션 정보
     */
    public record OptionInfo(
            Long optionNo,
            String optionName,
            Integer stockQty,
            Integer additionalPrice
    ) {
        public static OptionInfo from(ProductOption entity) {
            return new OptionInfo(
                    entity.getId(),
                    entity.getOptionName(),
                    entity.getStockCnt(),
                    entity.getAdditionalPrice()
            );
        }
    }
}
