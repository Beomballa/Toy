package com.section.admin.order.res;

import java.util.List;

public record AdminOrderClaimListResponse(
        List<Item> claims,
        int page,
        int totalPages,
        long totalElements,
        boolean hasNext
) {
    public record Item(
            long claimNo,
            long orderNo,
            String orderNumber,
            long memberNo,
            String claimType,
            String claimTypeLabel,
            String status,
            String statusLabel,
            String reason,
            String requestedAt
    ) {
    }
}
