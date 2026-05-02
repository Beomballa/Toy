package com.section.admin.product.req;

import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListReqDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductListReqDtoTest {

    @Test
    @DisplayName("상품 목록 요청은 상태와 정렬 조건을 typed query로 변환한다")
    void toQueryReturnsTypedQuery() {
        ProductListReqDto reqDto = new ProductListReqDto();
        reqDto.setCategoryNo(3L);
        reqDto.setBrandNo(7L);
        reqDto.setStatus("active");
        reqDto.setSearchKeyword("  젤   카야노 14 ");
        reqDto.setOrderType("c");
        reqDto.setLowStockOnly(true);
        reqDto.setCreatedTodayOnly(true);

        ProductListQuery query = reqDto.toQuery();

        assertEquals(3L, query.categoryNo());
        assertEquals(7L, query.brandNo());
        assertEquals(ProductStatus.ACTIVE, query.status());
        assertEquals("젤 카야노 14", query.searchKeyword());
        assertEquals(ProductOrderType.STOCK_COUNT, query.orderType());
        assertEquals(true, query.lowStockOnly());
        assertEquals(true, query.createdTodayOnly());
    }

    @Test
    @DisplayName("빈 상품 목록 필터는 기본 정렬과 null 조건으로 정규화된다")
    void toQueryNormalizesEmptyFilters() {
        ProductListReqDto reqDto = new ProductListReqDto();

        ProductListQuery query = reqDto.toQuery();

        assertNull(query.categoryNo());
        assertNull(query.brandNo());
        assertNull(query.status());
        assertNull(query.searchKeyword());
        assertEquals(ProductOrderType.RECENT, query.orderType());
        assertEquals(false, query.lowStockOnly());
        assertEquals(false, query.createdTodayOnly());
    }

    @Test
    @DisplayName("잘못된 상품 상태는 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsBusinessExceptionWhenStatusInvalid() {
        ProductListReqDto reqDto = new ProductListReqDto();
        reqDto.setStatus("UNKNOWN");

        BusinessException exception = assertThrows(BusinessException.class, reqDto::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("잘못된 상품 정렬 코드는 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsBusinessExceptionWhenOrderTypeInvalid() {
        ProductListReqDto reqDto = new ProductListReqDto();
        reqDto.setOrderType("latest");

        BusinessException exception = assertThrows(BusinessException.class, reqDto::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("검색어가 50자를 초과하면 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsBusinessExceptionWhenKeywordTooLong() {
        ProductListReqDto reqDto = new ProductListReqDto();
        reqDto.setSearchKeyword("a".repeat(51));

        BusinessException exception = assertThrows(BusinessException.class, reqDto::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("0번 브랜드나 카테고리 필터는 null 조건으로 정규화된다")
    void toQueryNormalizesZeroFilterIdsToNull() {
        ProductListReqDto reqDto = new ProductListReqDto();
        reqDto.setCategoryNo(0L);
        reqDto.setBrandNo(0L);

        ProductListQuery query = reqDto.toQuery();

        assertNull(query.categoryNo());
        assertNull(query.brandNo());
    }

    @Test
    @DisplayName("음수 브랜드나 카테고리 필터는 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsBusinessExceptionWhenFilterIdNegative() {
        ProductListReqDto reqDto = new ProductListReqDto();
        reqDto.setBrandNo(-1L);

        BusinessException exception = assertThrows(BusinessException.class, reqDto::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
