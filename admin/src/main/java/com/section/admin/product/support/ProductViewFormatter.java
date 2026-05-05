package com.section.admin.product.support;

import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.util.DateUtil;

import java.time.LocalDateTime;

public final class ProductViewFormatter {

    private ProductViewFormatter() {
    }

    public static String formatCreatedAt(LocalDateTime createdAt) {
        return createdAt == null ? "" : DateUtil.localDateTimeToStr(createdAt);
    }

    public static String formatExportedAt(LocalDateTime exportedAt) {
        return exportedAt == null ? "" : DateUtil.localDateTimeToStr(exportedAt);
    }

    public static String formatOrderType(ProductOrderType orderType) {
        if (orderType == null) {
            return "최신순";
        }

        return switch (orderType) {
            case RECENT -> "최신순";
            case RELEASE_PRICE -> "발매가순";
            case STOCK_COUNT -> "재고순";
        };
    }
}
