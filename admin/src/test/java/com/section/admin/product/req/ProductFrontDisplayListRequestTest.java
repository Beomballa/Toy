package com.section.admin.product.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.entity.type.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductFrontDisplayListRequestTest {

    @Test
    @DisplayName("프론트 노출 목록 요청은 필터와 정렬 값을 운영 기준으로 정규화한다")
    void normalizesFilterValues() {
        ProductFrontDisplayListRequest request = new ProductFrontDisplayListRequest(
                " Grey ",
                "active",
                7L,
                11L,
                "configured",
                "ready",
                true,
                true,
                15,
                "price_low"
        );

        assertEquals("Grey", request.normalizedKeyword());
        assertEquals(ProductStatus.ACTIVE, request.normalizedStatus());
        assertEquals(7L, request.normalizedBrandNo());
        assertEquals(11L, request.normalizedCategoryNo());
        assertEquals(true, request.normalizedConfigured());
        assertEquals("READY", request.normalizedContentStatus());
        assertTrue(request.normalizedFeaturedOnly());
        assertTrue(request.normalizedLowStockOnly());
        assertEquals(15L, request.normalizedLowStockThreshold(20));
        assertEquals("PRICE_LOW", request.normalizedSort());
    }

    @Test
    @DisplayName("프론트 노출 목록 요청은 비어있거나 잘못된 선택값을 기본값으로 되돌린다")
    void fallsBackForBlankValues() {
        ProductFrontDisplayListRequest request = new ProductFrontDisplayListRequest(
                "   ",
                "",
                0L,
                0L,
                "ALL",
                "ALL",
                null,
                null,
                0,
                "unknown"
        );

        assertNull(request.normalizedKeyword());
        assertNull(request.normalizedStatus());
        assertNull(request.normalizedBrandNo());
        assertNull(request.normalizedCategoryNo());
        assertNull(request.normalizedConfigured());
        assertNull(request.normalizedContentStatus());
        assertEquals(30L, request.normalizedLowStockThreshold(30));
        assertEquals("FEATURED", request.normalizedSort());
    }

    @Test
    @DisplayName("프론트 노출 목록 요청은 잘못된 상태값이면 INVALID_INPUT_VALUE 예외를 던진다")
    void throwsWhenStatusInvalid() {
        ProductFrontDisplayListRequest request = new ProductFrontDisplayListRequest(
                null,
                "invalid",
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                null
        );

        assertThrows(BusinessException.class, request::normalizedStatus);
    }

    @Test
    @DisplayName("프론트 노출 목록 요청은 잘못된 전시 설정값이나 음수 필터면 INVALID_INPUT_VALUE 예외를 던진다")
    void throwsWhenConfiguredOrFilterInvalid() {
        ProductFrontDisplayListRequest invalidConfiguredRequest = new ProductFrontDisplayListRequest(
                null,
                null,
                null,
                null,
                "maybe",
                "READY",
                false,
                false,
                null,
                null
        );
        ProductFrontDisplayListRequest invalidBrandRequest = new ProductFrontDisplayListRequest(
                null,
                null,
                -1L,
                null,
                null,
                null,
                false,
                false,
                null,
                null
        );
        ProductFrontDisplayListRequest invalidContentStatusRequest = new ProductFrontDisplayListRequest(
                null,
                null,
                null,
                null,
                null,
                "draft",
                false,
                false,
                null,
                null
        );

        assertThrows(BusinessException.class, invalidConfiguredRequest::normalizedConfigured);
        assertThrows(BusinessException.class, invalidBrandRequest::normalizedBrandNo);
        assertThrows(BusinessException.class, invalidContentStatusRequest::normalizedContentStatus);
    }
}
