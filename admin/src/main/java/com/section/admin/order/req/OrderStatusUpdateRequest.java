package com.section.admin.order.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "주문 번호는 필수입니다.")
        Long orderNo,

        @NotBlank(message = "주문 상태는 필수입니다.")
        String status
) {
}
