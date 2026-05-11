package com.section.admin.product.res;

import com.section.common.commerce.entity.ProductChangeHistory;
import com.section.common.util.DateUtil;

public record ProductHistoryResponse(
        Long historyNo,
        String actionType,
        String actionLabel,
        String summary,
        String statusSnapshot,
        Integer optionCount,
        Long totalStock,
        Long actorNo,
        String actorName,
        String crtDtm
) {
    public static ProductHistoryResponse from(ProductChangeHistory history, String actorName) {
        return new ProductHistoryResponse(
                history.getHistoryNo(),
                history.getActionType().name(),
                history.getActionType().getDesc(),
                history.getSummary(),
                history.getStatusSnapshot(),
                history.getOptionCount(),
                history.getTotalStock(),
                history.getCrtNo(),
                actorName,
                history.getCrtDtm() == null ? "" : DateUtil.localDateTimeToStr(history.getCrtDtm())
        );
    }
}
