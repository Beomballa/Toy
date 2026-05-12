package com.section.admin.order.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderMemoSaveRequest(
        @NotNull(message = "주문 번호는 필수입니다.")
        Long orderNo,

        @Size(max = 1000, message = "관리 메모는 1000자 이하여야 합니다.")
        String adminMemo
) {
    public String normalizedAdminMemo() {
        if (adminMemo == null) {
            return null;
        }
        String normalized = adminMemo.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
