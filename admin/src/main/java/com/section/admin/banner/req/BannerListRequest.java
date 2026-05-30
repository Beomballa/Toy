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
    private String exposureStatus;
    private Integer page = 0;
    private Integer size = 10;

    public BannerListQuery toQuery() {
        String normalizedActive = normalize(isActive);
        if (normalizedActive != null && !"Y".equalsIgnoreCase(normalizedActive) && !"N".equalsIgnoreCase(normalizedActive)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String normalizedExposureStatus = normalize(exposureStatus);
        if (normalizedExposureStatus != null
                && !"SCHEDULED".equalsIgnoreCase(normalizedExposureStatus)
                && !"LIVE".equalsIgnoreCase(normalizedExposureStatus)
                && !"ENDED".equalsIgnoreCase(normalizedExposureStatus)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new BannerListQuery(
                normalize(keyword),
                normalizedActive,
                normalizedExposureStatus == null ? null : normalizedExposureStatus.toUpperCase()
        );
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
        if (size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return size;
    }
}
