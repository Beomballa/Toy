package com.section.admin.order.support;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.util.DateUtil;

import java.time.LocalDateTime;

public final class OrderViewFormatter {
    private OrderViewFormatter() {
    }

    public static String formatAmount(Number amount) {
        long safeAmount = amount != null ? amount.longValue() : 0L;
        return String.format("%,d원", safeAmount);
    }

    public static String formatStatusDesc(String statusCode) {
        return OrderStatus.resolveDesc(statusCode);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? DateUtil.localDateTimeToStr(dateTime) : "";
    }

    public static String buildProductSummary(String firstProductName, Long itemCount) {
        if (firstProductName == null || firstProductName.isBlank()) {
            return "-";
        }
        if (itemCount == null || itemCount <= 1) {
            return firstProductName;
        }
        return firstProductName + " 외 " + (itemCount - 1) + "건";
    }
}
