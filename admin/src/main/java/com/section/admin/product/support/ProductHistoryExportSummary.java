package com.section.admin.product.support;

import com.section.common.base.entity.type.ProductHistoryActionType;
import com.section.common.base.entity.type.ProductHistoryOrderType;
import com.section.common.commerce.dto.ProductHistoryListQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record ProductHistoryExportSummary(
        String exportedAt,
        String orderTypeLabel,
        String filterSummary
) {
    public static ProductHistoryExportSummary from(ProductHistoryListQuery query) {
        return new ProductHistoryExportSummary(
                ProductViewFormatter.formatExportedAt(LocalDateTime.now()),
                resolveOrderTypeLabel(query.orderType()),
                buildFilterSummary(query)
        );
    }

    private static String resolveOrderTypeLabel(ProductHistoryOrderType orderType) {
        if (orderType == null) {
            return ProductHistoryOrderType.LATEST.getDesc();
        }
        return orderType.getDesc();
    }

    private static String buildFilterSummary(ProductHistoryListQuery query) {
        List<String> chunks = new ArrayList<>();
        if (query.productNo() != null) {
            chunks.add("상품번호: " + query.productNo());
        }
        if (query.actionType() != null) {
            chunks.add("작업유형: " + resolveActionTypeLabel(query.actionType()));
        }
        if (query.keyword() != null) {
            chunks.add("요약검색: " + query.keyword());
        }
        if (query.actorNo() != null) {
            chunks.add("작업자번호: " + query.actorNo());
        }
        if (query.actorKeyword() != null) {
            chunks.add("작업자명: " + query.actorKeyword());
        }
        if (query.startDate() != null || query.endDate() != null) {
            chunks.add("기간: " + formatDateRange(query.startDate(), query.endDate()));
        }
        return chunks.isEmpty() ? "추가 필터 없음" : String.join(" | ", chunks);
    }

    private static String resolveActionTypeLabel(ProductHistoryActionType actionType) {
        return switch (actionType) {
            case CREATED -> "등록";
            case UPDATED -> "수정";
            case DELETED -> "삭제";
        };
    }

    private static String formatDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return startDate + " ~ " + endDate;
        }
        if (startDate != null) {
            return startDate + " 이후";
        }
        return endDate + " 이전";
    }
}
