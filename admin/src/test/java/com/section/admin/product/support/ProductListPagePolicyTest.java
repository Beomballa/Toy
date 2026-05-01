package com.section.admin.product.support;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductListPagePolicyTest {

    @Test
    @DisplayName("상품 목록 페이지 크기는 10, 20, 50만 허용한다")
    void normalizeReturnsSamePageableWhenSizeAllowed() {
        Pageable pageable = PageRequest.of(1, 20);

        Pageable normalizedPageable = ProductListPagePolicy.normalize(pageable);

        assertEquals(1, normalizedPageable.getPageNumber());
        assertEquals(20, normalizedPageable.getPageSize());
    }

    @Test
    @DisplayName("허용되지 않은 상품 목록 페이지 크기는 INVALID_INPUT_VALUE 예외를 던진다")
    void normalizeThrowsBusinessExceptionWhenSizeNotAllowed() {
        Pageable pageable = PageRequest.of(0, 15);

        BusinessException exception = assertThrows(BusinessException.class, () -> ProductListPagePolicy.normalize(pageable));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
