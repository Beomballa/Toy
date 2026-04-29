package com.section.admin.order.support;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderListPagePolicyTest {

    @Test
    @DisplayName("허용된 페이지 크기는 그대로 통과한다")
    void normalizeReturnsPageableWhenSizeAllowed() {
        Pageable pageable = PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "id"));

        Pageable normalized = OrderListPagePolicy.normalize(pageable);

        assertEquals(1, normalized.getPageNumber());
        assertEquals(20, normalized.getPageSize());
        assertEquals(pageable.getSort(), normalized.getSort());
    }

    @Test
    @DisplayName("허용되지 않은 페이지 크기는 INVALID_INPUT_VALUE 예외를 던진다")
    void normalizeThrowsBusinessExceptionWhenSizeNotAllowed() {
        Pageable pageable = PageRequest.of(0, 15);

        BusinessException exception = assertThrows(BusinessException.class, () -> OrderListPagePolicy.normalize(pageable));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
