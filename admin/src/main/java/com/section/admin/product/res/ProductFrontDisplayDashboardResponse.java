package com.section.admin.product.res;

import java.util.List;

public record ProductFrontDisplayDashboardResponse(
        ProductFrontDisplaySummaryResponse summary,
        List<ProductFrontDisplayListResponse> items
) {
}
