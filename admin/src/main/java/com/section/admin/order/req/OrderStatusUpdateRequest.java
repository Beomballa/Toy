package com.section.admin.order.req;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderStatusUpdateRequest(
        @NotNull(message = "주문 번호는 필수입니다.")
        Long orderNo,

        @NotBlank(message = "주문 상태는 필수입니다.")
        String status,

        @Size(max = 200, message = "사유는 200자 이하여야 합니다.")
        String reason
) {
    public OrderStatus toOrderStatus() {
        try {
            // 상태 변경은 화면 문자열이 아닌 enum 경계로 먼저 수렴시켜야 후속 상태 전이가 안정적입니다.
            return OrderStatus.valueOf(this.status());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public String normalizedReason() {
        if (reason == null) {
            return null;
        }
        String normalized = reason.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
