package com.section.admin.order.req;

import jakarta.validation.constraints.NotNull;

public record OrderNoRequest(
        @NotNull(message = "주문 번호는 필수입니다.")
        Long orderNo
) {
}
