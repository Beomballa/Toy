package com.section.admin.order.req;

import com.section.common.commerce.entity.FrontOrderClaimStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminOrderClaimStatusUpdateRequest(
        @NotNull Long claimNo,
        @NotNull FrontOrderClaimStatus status,
        @NotBlank @Size(max = 1000) String memo
) {
    public String normalizedMemo() {
        return memo.trim().replaceAll("\\s+", " ");
    }
}
