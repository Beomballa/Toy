package com.section.admin.brand.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrandListRequestTest {

    @Test
    @DisplayName("브랜드 목록 요청은 상태 필터를 YN으로 정규화한다")
    void normalizedIsActiveUppercasesYnValue() {
        BrandListRequest request = new BrandListRequest();
        request.setIsActive(" y ");

        assertEquals("Y", request.normalizedIsActive());
    }

    @Test
    @DisplayName("브랜드 목록 요청은 잘못된 상태 필터를 거부한다")
    void normalizedIsActiveRejectsInvalidValue() {
        BrandListRequest request = new BrandListRequest();
        request.setIsActive("ACTIVE");

        BusinessException exception = assertThrows(BusinessException.class, request::normalizedIsActive);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
