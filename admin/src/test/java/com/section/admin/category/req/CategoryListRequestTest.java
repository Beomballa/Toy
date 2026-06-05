package com.section.admin.category.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CategoryListRequestTest {

    @Test
    @DisplayName("카테고리 목록 요청은 depth 기본값을 1로 정규화한다")
    void normalizedDepthDefaultsToOne() {
        CategoryListRequest request = new CategoryListRequest();
        request.setDepth(null);

        assertEquals(1, request.normalizedDepth());
    }

    @Test
    @DisplayName("카테고리 목록 요청은 잘못된 depth를 거부한다")
    void normalizedDepthRejectsInvalidValue() {
        CategoryListRequest request = new CategoryListRequest();
        request.setDepth(3);

        BusinessException exception = assertThrows(BusinessException.class, request::normalizedDepth);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("카테고리 목록 요청은 상태 필터를 YN으로 정규화한다")
    void normalizedIsActiveUppercasesYnValue() {
        CategoryListRequest request = new CategoryListRequest();
        request.setIsActive(" n ");

        assertEquals("N", request.normalizedIsActive());
    }

    @Test
    @DisplayName("카테고리 목록 요청은 잘못된 상태 필터를 거부한다")
    void normalizedIsActiveRejectsInvalidValue() {
        CategoryListRequest request = new CategoryListRequest();
        request.setIsActive("enabled");

        BusinessException exception = assertThrows(BusinessException.class, request::normalizedIsActive);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
