package com.section.admin.product.res;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ProductHistoryRelatedProductSupport {
    private static final Pattern SOURCE_PRODUCT_NO_PATTERN = Pattern.compile("원본 상품 번호:\\s*(\\d+)");

    private ProductHistoryRelatedProductSupport() {
    }

    static Long resolveRelatedProductNo(String summary) {
        if (summary == null || summary.isBlank()) {
            return null;
        }

        // 복제 이력은 별도 연관 컬럼이 없어서 summary에 남긴 원본 상품 번호를 파생해서 사용합니다.
        Matcher matcher = SOURCE_PRODUCT_NO_PATTERN.matcher(summary);
        if (!matcher.find()) {
            return null;
        }

        return Long.parseLong(matcher.group(1));
    }

    static String resolveRelatedProductLabel(String summary) {
        return resolveRelatedProductNo(summary) == null ? null : "원본 상품";
    }
}
