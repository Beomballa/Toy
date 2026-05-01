package com.section.admin.dashboard.res;

import com.section.common.commerce.dto.OrderListResDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardResponseTest {

    @Test
    @DisplayName("최근 주문 응답은 주문 상세 화면과 같은 포맷 규칙을 따른다")
    void recentOrderFromUsesSharedOrderFormatters() {
        OrderListResDto dto = OrderListResDto.builder()
                .orderNo(1L)
                .orderNum("ORD-1")
                .buyerName("함장님")
                .totalAmount(348000)
                .status("PAID")
                .crtDtm(LocalDateTime.of(2026, 5, 1, 9, 30))
                .build();

        DashboardResponse.RecentOrder result = DashboardResponse.RecentOrder.from(dto);

        assertEquals(1L, result.orderNo());
        assertEquals("ORD-1", result.orderNum());
        assertEquals("함장님", result.buyerName());
        assertEquals("348,000원", result.totalAmount());
        assertEquals("결제완료", result.statusDesc());
        assertEquals("PAID", result.statusCode());
        assertEquals("2026.05.01 09:30", result.orderDt());
    }
}
