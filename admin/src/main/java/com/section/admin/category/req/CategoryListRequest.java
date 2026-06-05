package com.section.admin.category.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
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

    public int normalizedDepth() {
        int normalized = depth == null ? 1 : depth;
        if (normalized != 1 && normalized != 2) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    public String normalizedKeyword() {
        return normalize(keyword);
    }

    public String normalizedIsActive() {
        String normalized = normalize(isActive);
        if (normalized == null) {
            return null;
        }
        if (!"Y".equalsIgnoreCase(normalized) && !"N".equalsIgnoreCase(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized.toUpperCase();
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
