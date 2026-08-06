package com.section.front.commerce.dto;

import java.util.List;

public record FrontMemberOrderListResponse(
        List<FrontMemberOrderItemResponse> items,
        int page,
        int size,
        int totalPages,
        long totalElements,
        boolean hasNext,
        String selectedStatus,
        java.util.List<FrontMemberOrderStatusSummary> statusSummaries
) {
}
