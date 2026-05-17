package com.section.admin.product.res;

import com.section.admin.log.res.AdminLogSourceLinkSupport;
import com.section.common.commerce.entity.ProductChangeHistory;
import com.section.common.util.DateUtil;

public record ProductHistoryResponse(
        Long historyNo,
        Long relatedProductNo,
        String relatedProductLabel,
        String activityLogPath,
        String activityLogLabel,
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
        Long relatedProductNo = ProductHistoryRelatedProductSupport.resolveRelatedProductNo(history.getSummary());
        return new ProductHistoryResponse(
                history.getHistoryNo(),
                relatedProductNo,
                ProductHistoryRelatedProductSupport.resolveRelatedProductLabel(history.getSummary()),
                AdminLogSourceLinkSupport.resolveProductHistoryLogPath(history.getProductNo(), history.getActionType().name()),
                "활동 로그 보기",
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
