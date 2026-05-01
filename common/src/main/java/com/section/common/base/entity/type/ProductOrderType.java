package com.section.common.base.entity.type;

public enum ProductOrderType {
    RECENT("r"),
    RELEASE_PRICE("p"),
    STOCK_COUNT("c");

    private final String code;

    ProductOrderType(String code) {
        this.code = code;
    }

    public static ProductOrderType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return RECENT;
        }

        for (ProductOrderType value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }

        return null;
    }
}
