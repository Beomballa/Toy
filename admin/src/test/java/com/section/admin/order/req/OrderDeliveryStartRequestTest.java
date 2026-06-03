package com.section.admin.order.req;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderDeliveryStartRequestTest {

    @Test
    @DisplayName("배송 시작 요청은 택배사와 운송장 번호 앞뒤 공백을 제거한다")
    void normalizeDeliveryFields() {
        OrderDeliveryStartRequest request = new OrderDeliveryStartRequest(
                1L,
                "  CJ대한통운  ",
                "  1234-5678-9999  ",
                "  출고 요청  "
        );

        assertEquals("CJ대한통운", request.normalizedDeliveryCompany());
        assertEquals("1234-5678-9999", request.normalizedTrackingNum());
        assertEquals("출고 요청", request.normalizedReason());
    }

    @Test
    @DisplayName("배송 시작 요청은 운송장 번호 중간 공백도 제거한다")
    void normalizeTrackingNumRemovesInnerSpaces() {
        OrderDeliveryStartRequest request = new OrderDeliveryStartRequest(
                1L,
                "CJ대한통운",
                "12 34 - 56 78",
                null
        );

        assertEquals("1234-5678", request.normalizedTrackingNum());
    }
}
