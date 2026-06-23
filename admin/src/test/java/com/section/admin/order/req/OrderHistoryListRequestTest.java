package com.section.admin.order.req;

import com.section.common.base.entity.type.OrderHistoryOrderType;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
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
        request.setActorNo(77L);
        request.setActorKeyword("  관리자  ");
        request.setStartDate(LocalDate.of(2026, 5, 1));
        request.setEndDate(LocalDate.of(2026, 5, 31));
        request.setOrderType("oldest");

        OrderHistoryListQuery query = request.toQuery();

        assertEquals(9L, query.orderNo());
        assertEquals("DELIVERY_START", query.actionType());
        assertEquals("cj 1234", query.keyword());
        assertEquals(77L, query.actorNo());
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

    @Test
    @DisplayName("주문 이력 작업자 번호가 0 이하이면 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsWhenActorNoInvalid() {
        OrderHistoryListRequest request = new OrderHistoryListRequest();
        request.setActorNo(0L);

        BusinessException exception = assertThrows(BusinessException.class, request::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("주문 이력 기간이 92일을 초과하면 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsWhenDateRangeTooLarge() {
        OrderHistoryListRequest request = new OrderHistoryListRequest();
        request.setStartDate(LocalDate.of(2026, 1, 1));
        request.setEndDate(LocalDate.of(2026, 4, 10));

        BusinessException exception = assertThrows(BusinessException.class, request::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("주문 이력 검색어가 50자를 초과하면 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsWhenKeywordTooLong() {
        OrderHistoryListRequest request = new OrderHistoryListRequest();
        request.setKeyword("a".repeat(51));

        BusinessException exception = assertThrows(BusinessException.class, request::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("주문 이력 작업자 검색어가 50자를 초과하면 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsWhenActorKeywordTooLong() {
        OrderHistoryListRequest request = new OrderHistoryListRequest();
        request.setActorKeyword("a".repeat(51));

        BusinessException exception = assertThrows(BusinessException.class, request::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
