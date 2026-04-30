package com.section.admin.order.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderViewFormatterTest {

    @Test
    @DisplayName("주문 화면 공통 포맷은 금액, 상태, 상품 요약을 일관되게 변환한다")
    void formatterReturnsConsistentOrderViewValues() {
        assertEquals("12,345원", OrderViewFormatter.formatAmount(12345));
        assertEquals("결제완료", OrderViewFormatter.formatStatusDesc("PAID"));
        assertEquals("UNKNOWN", OrderViewFormatter.formatStatusDesc("UNKNOWN"));
        assertEquals("삼바 외 2건", OrderViewFormatter.buildProductSummary("삼바", 3L));
        assertEquals("-", OrderViewFormatter.buildProductSummary("", 3L));
        assertEquals("2026.04.30 18:30", OrderViewFormatter.formatDateTime(LocalDateTime.of(2026, 4, 30, 18, 30)));
    }
}
