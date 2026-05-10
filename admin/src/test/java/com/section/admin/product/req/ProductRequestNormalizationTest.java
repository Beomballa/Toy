package com.section.admin.product.req;

import com.section.admin.product.support.ProductInputNormalizer;
import com.section.common.commerce.dto.ProductCreateReqDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductRequestNormalizationTest {

    @Test
    @DisplayName("상품 생성 요청은 이름과 선택 필드를 저장 전에 정규화한다")
    void productCreateRequestNormalizesFields() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryNo(1L);
        request.setBrandNo(2L);
        request.setNameKo("  젤   카야노  14  ");
        request.setModelNum("  1201A019  ");
        request.setReleasePrice(129000);
        request.setThumbnailUrl("   ");

        ProductCreateReqDto dto = request.toProductCreateReqDto();

        assertEquals("젤 카야노 14", dto.getNameKo());
        assertEquals("1201A019", dto.getModelNum());
        assertNull(dto.getThumbnailUrl());
    }

    @Test
    @DisplayName("상품 수정 요청은 선택 필드 공백을 null로 정규화한다")
    void productUpdateRequestNormalizesOptionalFields() {
        ProductUpdateRequest request = new ProductUpdateRequest();

        assertEquals("젤 카야노 14", request.normalizeRequiredText("  젤   카야노  14 "));
        assertEquals("1201A019", request.normalizeOptionalText("  1201A019 "));
        assertNull(request.normalizeOptionalText("   "));
    }

    @Test
    @DisplayName("상품 옵션명은 다중 공백을 정규화한다")
    void productOptionNameNormalizesWhitespace() {
        ProductCreateRequest.ProductOptionRequest createOption = new ProductCreateRequest.ProductOptionRequest();
        createOption.setOptionName("  270   wide ");

        ProductUpdateRequest.ProductOptionUpdateRequest updateOption = new ProductUpdateRequest.ProductOptionUpdateRequest();
        updateOption.setOptionName("  280   standard ");

        assertEquals("270 wide", createOption.normalizeOptionName());
        assertEquals("280 standard", updateOption.normalizeOptionName());
    }

    @Test
    @DisplayName("상품 입력 정규화 유틸은 필수값과 선택값을 같은 기준으로 처리한다")
    void productInputNormalizerNormalizesRequiredAndOptionalText() {
        assertEquals("젤 카야노 14", ProductInputNormalizer.normalizeRequiredText("  젤   카야노  14 "));
        assertEquals("1201A019", ProductInputNormalizer.normalizeOptionalText("  1201A019 "));
        assertNull(ProductInputNormalizer.normalizeOptionalText("   "));
        assertEquals("", ProductInputNormalizer.normalizeRequiredText(null));
    }
}
