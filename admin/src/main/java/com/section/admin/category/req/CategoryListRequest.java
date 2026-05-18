package com.section.admin.category.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryListRequest {

    private Integer depth = 1;
    private String keyword;
    private String isActive;
    private Integer page = 0;
    private Integer size = 10;

    public String normalizedKeyword() {
        return normalize(keyword);
    }

    public String normalizedIsActive() {
        return normalize(isActive);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    public int normalizedPage() {
        return page == null || page < 0 ? 0 : page;
    }

    public int normalizedSize() {
        if (size == null || size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
