package com.section.admin.banner.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.BannerListQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BannerListRequestTest {

    @Test
    @DisplayName("배너 목록 요청은 노출 기간 필터를 정규화한다")
    void toQueryNormalizesExposureStatus() {
        BannerListRequest request = new BannerListRequest();
        request.setKeyword("  메인   프로모션 ");
        request.setExposureStatus(" live ");

        BannerListQuery query = request.toQuery();

        assertEquals("메인 프로모션", query.keyword());
        assertEquals("LIVE", query.exposureStatus());
    }

    @Test
    @DisplayName("배너 목록 요청은 잘못된 노출 기간 필터를 거부한다")
    void toQueryRejectsInvalidExposureStatus() {
        BannerListRequest request = new BannerListRequest();
        request.setExposureStatus("UNKNOWN");

        BusinessException exception = assertThrows(BusinessException.class, request::toQuery);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
