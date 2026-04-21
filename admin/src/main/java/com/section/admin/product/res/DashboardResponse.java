package com.section.admin.product.res;

import java.util.List;

public record DashboardResponse(
        SummaryCounts summary,
        List<RecentOrder> recentOrders,
        List<LowStockProduct> lowStockProducts,
        List<ChartData> salesChart
) {
    public record SummaryCounts(
            long todayOrderCount,
            String todayTotalAmount,
            long preparingCount,
            long shippingCount,
            long cancelledCount
    ) {}

    public record RecentOrder(
            Long orderNo,
            String orderNum,
            String buyerName,
            String totalAmount,
            String statusDesc,
            String orderDt
    ) {}

    public record LowStockProduct(
            Long productNo,
            String productName,
            String brandName,
            long stockCnt
    ) {}

    public record ChartData(
            String label,
            long value
    ) {}
}
