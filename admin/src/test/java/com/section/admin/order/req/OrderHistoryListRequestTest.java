package com.section.admin.order.req;

import com.section.common.base.entity.type.OrderHistoryOrderType;
import com.section.common.base.exception.BusinessException;
import com.section.common.commerce.dto.OrderHistoryListQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderHistoryListRequestTest {

    @Test
    @DisplayName("주문 이력 목록 요청은 검색어와 정렬을 정규화한다")
    void toQueryNormalizesInput() {
        OrderHistoryListRequest request = new OrderHistoryListRequest();
        request.setOrderNo(9L);
        request.setActionType("delivery_start");
        request.setKeyword("  cj   1234  ");
        request.setActorKeyword("  관리자  ");
        request.setStartDate(LocalDate.of(2026, 5, 1));
        request.setEndDate(LocalDate.of(2026, 5, 31));
        request.setOrderType("oldest");

        OrderHistoryListQuery query = request.toQuery();

        assertEquals(9L, query.orderNo());
        assertEquals("DELIVERY_START", query.actionType());
        assertEquals("cj 1234", query.keyword());
        assertEquals("관리자", query.actorKeyword());
        assertEquals(OrderHistoryOrderType.OLDEST, query.orderType());
    }

    @Test
    @DisplayName("주문 이력 목록 요청은 빈 값이면 기본 정렬을 최신순으로 사용한다")
    void toQueryUsesLatestAsDefault() {
        OrderHistoryListRequest request = new OrderHistoryListRequest();

        OrderHistoryListQuery query = request.toQuery();

        assertNull(query.orderNo());
        assertEquals(OrderHistoryOrderType.LATEST, query.orderType());
    }

    @Test
    @DisplayName("주문 이력 목록 요청은 잘못된 작업 유형이면 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsWhenActionTypeInvalid() {
        OrderHistoryListRequest request = new OrderHistoryListRequest();
        request.setActionType("unknown");

        assertThrows(BusinessException.class, request::toQuery);
    }
}
