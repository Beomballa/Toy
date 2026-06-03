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

    public static String formatActionLabel(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return "상태 변경";
        }

        return switch (actionType) {
            case "STATUS_CHANGE" -> "상태 변경";
            case "DELIVERY_START" -> "배송 시작";
            case "DELIVERY_COMPLETE" -> "배송 완료";
            case "CANCEL" -> "주문 취소";
            case "ADMIN_MEMO" -> "메모 저장";
            default -> actionType;
        };
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
