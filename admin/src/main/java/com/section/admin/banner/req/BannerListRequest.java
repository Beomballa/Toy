package com.section.admin.banner.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.BannerListQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BannerListRequest {

    private String keyword;
    private String isActive;

    public BannerListQuery toQuery() {
        String normalizedActive = normalize(isActive);
        if (normalizedActive != null && !"Y".equalsIgnoreCase(normalizedActive) && !"N".equalsIgnoreCase(normalizedActive)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new BannerListQuery(normalize(keyword), normalizedActive);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
