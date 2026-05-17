package com.section.admin.log.res;

import com.section.common.base.entity.type.ProductHistoryActionType;

public final class AdminLogSourceLinkSupport {

    private AdminLogSourceLinkSupport() {
    }

    public static String resolveProductHistoryLogPath(Long productNo, String actionType) {
        if (productNo == null) {
            return null;
        }
        String resolvedActionType = switch (ProductHistoryActionType.valueOf(actionType)) {
            case CREATED -> "PRODUCT_CREATE";
            case UPDATED -> "PRODUCT_UPDATE";
            case DELETED -> "PRODUCT_DELETE";
        };
        return "/admin/logs?actionType=" + resolvedActionType + "&targetId=" + productNo;
    }

    public static String resolveOrderHistoryLogPath(Long orderNo, String actionType) {
        if (orderNo == null || actionType == null || actionType.isBlank()) {
            return null;
        }
        String resolvedActionType = switch (actionType) {
            case "STATUS_CHANGE" -> "ORDER_STATUS_CHANGE";
            case "DELIVERY_START" -> "ORDER_DELIVERY_START";
            case "DELIVERY_COMPLETE" -> "ORDER_DELIVERY_COMPLETE";
            case "CANCEL" -> "ORDER_CANCEL";
            case "ADMIN_MEMO" -> "ORDER_ADMIN_MEMO";
            default -> null;
        };
        if (resolvedActionType == null) {
            return null;
        }
        return "/admin/logs?actionType=" + resolvedActionType + "&targetId=" + orderNo;
    }
}
