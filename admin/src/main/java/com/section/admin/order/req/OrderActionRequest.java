package com.section.admin.order.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderActionRequest(
        @NotNull(message = "주문 번호는 필수입니다.")
        Long orderNo,

        @Size(max = 200, message = "사유는 200자 이하여야 합니다.")
        String reason
) {
    public String normalizedReason() {
        if (reason == null) {
            return null;
        }
        String normalized = reason.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
