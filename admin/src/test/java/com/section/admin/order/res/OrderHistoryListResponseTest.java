package com.section.admin.order.res;

import com.section.common.base.entity.type.OrderHistoryOrderType;
import com.section.common.commerce.dto.OrderHistoryListQuery;
import com.section.common.commerce.dto.OrderHistoryListResDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderHistoryListResponseTest {

    @Test
    @DisplayName("주문 이력 페이지 라벨은 전체 페이지 수 기준으로 계산한다")
    void ofUsesTotalPageCount() {
        OrderHistoryListResDto row = new OrderHistoryListResDto();
        row.setHistoryNo(1L);
        row.setOrderNo(7L);
        row.setActionType("DELIVERY_START");
        row.setActionDtm(LocalDateTime.of(2026, 6, 6, 11, 0));

        OrderHistoryListResponse response = OrderHistoryListResponse.of(
                new PageImpl<>(List.of(row), PageRequest.of(1, 10), 21),
                new OrderHistoryListQuery(7L, null, null, null, null, null, OrderHistoryOrderType.LATEST)
        );

        assertEquals("11-11 / 21건 · 3페이지", response.pageInfoLabel());
        assertEquals("11-11 / 21건 · 3페이지", response.resultMeta().pageInfoLabel());
    }
}
