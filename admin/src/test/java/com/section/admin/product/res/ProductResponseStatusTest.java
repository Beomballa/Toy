package com.section.admin.product.res;

import com.section.common.commerce.dto.ProductDetailResDto;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.entity.ProductOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductResponseStatusTest {

    @Test
    @DisplayName("상품 목록 응답은 상태 코드를 화면용 상태명으로 변환한다")
    void productListItemMapsStatusDesc() {
        ProductListResDto dto = new ProductListResDto();
        dto.setStatus("SOLD_OUT");
        dto.setReleasePrice(129000);

        ProductListResponse.ProductListItem item = ProductListResponse.ProductListItem.from(dto);

        assertEquals("SOLD_OUT", item.statusCode());
        assertEquals("품절", item.statusDesc());
    }

    @Test
    @DisplayName("상품 상세 응답은 상태 코드를 화면용 상태명으로 변환한다")
    void productDetailResponseMapsStatusDesc() {
        ProductDetailResDto dto = new ProductDetailResDto(
                1L, 2L, "러닝화", 3L, "아식스", "젤 카야노 14",
                "1201A019", 129000, null, null, "HIDDEN", null, null
        );

        ProductOption firstOption = ProductOption.builder().optionName("260").stockCnt(2).additionalPrice(0).build();
        ProductOption secondOption = ProductOption.builder().optionName("270").stockCnt(3).additionalPrice(5000).build();

        ProductDetailResponse response = ProductDetailResponse.from(dto, List.of(firstOption, secondOption));

        assertEquals("HIDDEN", response.statusCode());
        assertEquals("숨김", response.statusDesc());
        assertEquals(2, response.optionCount());
        assertEquals(5L, response.totalStock());
        assertEquals(false, response.hasThumbnail());
    }
}
