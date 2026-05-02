package com.section.admin.product.support;

import com.section.common.util.DateUtil;

import java.time.LocalDateTime;

public final class ProductViewFormatter {

    private ProductViewFormatter() {
    }

    public static String formatCreatedAt(LocalDateTime createdAt) {
        return createdAt == null ? "" : DateUtil.localDateTimeToStr(createdAt);
    }
}
