package com.section.common.commerce.entity;

public enum FrontMemberActivityType {
    BOOKMARK(24),
    COMPARE(3),
    RECENT(12),
    HIDDEN(12);

    private final int limit;

    FrontMemberActivityType(int limit) {
        this.limit = limit;
    }

    public int limit() {
        return limit;
    }
}
