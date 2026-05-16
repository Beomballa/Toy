package com.section.admin.order.req;

import com.section.common.base.entity.type.OrderHistoryOrderType;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.OrderHistoryListQuery;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class OrderHistoryListRequest {
    private static final Set<String> ALLOWED_ACTION_TYPES = Set.of(
            "STATUS_CHANGE",
            "DELIVERY_START",
            "DELIVERY_COMPLETE",
            "CANCEL",
            "ADMIN_MEMO"
    );

    private Long orderNo;
    private String actionType;
    private String keyword;
    private String actorKeyword;
    private LocalDate startDate;
    private LocalDate endDate;
    private String orderType;

    public OrderHistoryListQuery toQuery() {
        if (orderNo != null && orderNo <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return new OrderHistoryListQuery(
                orderNo,
                parseActionType(actionType),
                normalizeKeyword(keyword),
                normalizeKeyword(actorKeyword),
                startDate,
                endDate,
                parseOrderType(orderType)
        );
    }

    private String parseActionType(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return null;
        }
        String normalized = actionType.trim().toUpperCase();
        if (!ALLOWED_ACTION_TYPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private OrderHistoryOrderType parseOrderType(String orderType) {
        if (orderType == null || orderType.isBlank()) {
            return OrderHistoryOrderType.LATEST;
        }
        return switch (orderType.trim().toLowerCase()) {
            case "latest" -> OrderHistoryOrderType.LATEST;
            case "oldest" -> OrderHistoryOrderType.OLDEST;
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        };
    }
}
