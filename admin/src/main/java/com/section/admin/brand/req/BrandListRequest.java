package com.section.admin.brand.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandListRequest {

    private String keyword;
    private String isActive;

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
}
