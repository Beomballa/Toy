package com.section.admin.order.support;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public final class OrderListPagePolicy {

    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 20, 50);

    private OrderListPagePolicy() {
    }

    public static Pageable normalize(Pageable pageable) {
        int requestedSize = pageable.getPageSize();
        if (!ALLOWED_PAGE_SIZES.contains(requestedSize)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 목록 크기는 허용된 구간으로만 열어두어야 URL 조작과 UI 규칙이 어긋나지 않습니다.
        return PageRequest.of(pageable.getPageNumber(), requestedSize, pageable.getSort());
    }
}
