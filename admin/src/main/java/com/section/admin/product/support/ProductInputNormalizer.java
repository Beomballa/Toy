package com.section.admin.product.support;

public final class ProductInputNormalizer {

    private ProductInputNormalizer() {
    }

    public static String normalizeRequiredText(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    public static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return normalizeRequiredText(value);
    }
}
