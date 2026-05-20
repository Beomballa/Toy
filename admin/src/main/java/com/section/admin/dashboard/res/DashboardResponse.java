package com.section.admin.dashboard.res;

import com.section.admin.order.support.OrderViewFormatter;
import com.section.common.commerce.dto.OrderListResDto;

import java.util.List;

public record DashboardResponse(
        SummaryCounts summary,
        List<OperationNotice> operationNotices,
        List<RecentOrder> recentOrders,
        List<LowStockProduct> lowStockProducts,
        List<ChartData> salesChart,
        List<ChartData> topProducts,
        List<ChartData> topBrands
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
            String statusCode,
            String orderDt
    ) {
        public static RecentOrder from(OrderListResDto order) {
            return new RecentOrder(
                    order.getOrderNo(),
                    order.getOrderNum(),
                    order.getBuyerName(),
                    OrderViewFormatter.formatAmount(order.getTotalAmount()),
                    OrderViewFormatter.formatStatusDesc(order.getStatus()),
                    order.getStatus(),
                    OrderViewFormatter.formatDateTime(order.getCrtDtm())
            );
        }
    }

    public record LowStockProduct(
            Long productNo,
            String productName,
            String brandName,
            long stockCnt
    ) {}

    public record OperationNotice(
            Long noticeNo,
            String title,
            String content,
            boolean pinned,
            String periodLabel,
            String targetPath,
            String historyPath
    ) {}

    public record ChartData(
            String label,
            long value
    ) {}
}
