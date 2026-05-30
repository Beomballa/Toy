package com.section.admin.order.req;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.OrderListQuery;
import com.section.common.commerce.dto.OrderListReqDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderListReqDtoTest {

    @Test
    @DisplayName("주문 목록 요청은 상태와 기간을 typed query로 변환한다")
    void toQueryReturnsTypedQuery() {
        OrderListReqDto reqDto = new OrderListReqDto();
        reqDto.setStatus("PAID");
        reqDto.setSearchKeyword("  함장님   010-1234-5678  삼바  ");
        reqDto.setStartDate("2026-04-01");
        reqDto.setEndDate("2026-04-30");

        OrderListQuery query = reqDto.toQuery();

        assertEquals(OrderStatus.PAID, query.status());
        assertEquals("함장님 010-1234-5678 삼바", query.searchKeyword());
        assertEquals("2026-04-01T00:00", query.startDateTime().toString());
        assertEquals("2026-04-30T23:59:59.999999999", query.endDateTime().toString());
    }

    @Test
    @DisplayName("빈 필터는 null query 조건으로 정규화된다")
    void toQueryReturnsNullFiltersWhenEmpty() {
        OrderListReqDto reqDto = new OrderListReqDto();

        OrderListQuery query = reqDto.toQuery();

        assertNull(query.status());
        assertNull(query.searchKeyword());
        assertNull(query.startDateTime());
        assertNull(query.endDateTime());
    }

    @Test
    @DisplayName("잘못된 주문 상태는 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsBusinessExceptionWhenStatusInvalid() {
        OrderListReqDto reqDto = new OrderListReqDto();
        reqDto.setStatus("UNKNOWN");

        BusinessException exception = assertThrows(BusinessException.class, reqDto::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("시작일이 종료일보다 뒤면 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsBusinessExceptionWhenDateRangeInvalid() {
        OrderListReqDto reqDto = new OrderListReqDto();
        reqDto.setStartDate("2026-04-30");
        reqDto.setEndDate("2026-04-01");

        BusinessException exception = assertThrows(BusinessException.class, reqDto::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("조회 기간이 92일을 초과하면 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsBusinessExceptionWhenDateRangeTooLarge() {
        OrderListReqDto reqDto = new OrderListReqDto();
        reqDto.setStartDate("2026-01-01");
        reqDto.setEndDate("2026-04-10");

        BusinessException exception = assertThrows(BusinessException.class, reqDto::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("검색어가 50자를 초과하면 INVALID_INPUT_VALUE 예외를 던진다")
    void toQueryThrowsBusinessExceptionWhenKeywordTooLong() {
        OrderListReqDto reqDto = new OrderListReqDto();
        reqDto.setSearchKeyword("a".repeat(51));

        BusinessException exception = assertThrows(BusinessException.class, reqDto::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
