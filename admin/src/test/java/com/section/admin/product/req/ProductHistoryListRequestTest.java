package com.section.admin.product.req;

import com.section.common.base.entity.type.ProductHistoryOrderType;
import com.section.common.base.exception.BusinessException;
import com.section.common.commerce.dto.ProductHistoryListQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductHistoryListRequestTest {

    @Test
    @DisplayName("상품 이력 목록 요청은 작업자 검색어와 정렬을 정규화한다")
    void toQueryNormalizesActorKeywordAndOrderType() {
        ProductHistoryListRequest request = new ProductHistoryListRequest();
        request.setActorKeyword("  관리자  계정 ");
        request.setOrderType("oldest");

        ProductHistoryListQuery query = request.toQuery();

        assertEquals("관리자 계정", query.actorKeyword());
        assertEquals(ProductHistoryOrderType.OLDEST, query.orderType());
    }

    @Test
    @DisplayName("상품 이력 목록 요청은 잘못된 정렬값을 거부한다")
    void toQueryRejectsInvalidOrderType() {
        ProductHistoryListRequest request = new ProductHistoryListRequest();
        request.setOrderType("random");

        assertThrows(BusinessException.class, request::toQuery);
    }
}
