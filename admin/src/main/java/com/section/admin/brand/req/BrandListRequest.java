package com.section.admin.brand.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandListRequest {

    private String keyword;
    private String isActive;
    private Integer page = 0;
    private Integer size = 10;

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
