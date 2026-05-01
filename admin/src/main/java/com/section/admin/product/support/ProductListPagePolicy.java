package com.section.admin.product.support;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public final class ProductListPagePolicy {

    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 20, 50);

    private ProductListPagePolicy() {
    }

    public static Pageable normalize(Pageable pageable) {
        int pageSize = pageable.getPageSize();
        if (!ALLOWED_PAGE_SIZES.contains(pageSize)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return PageRequest.of(pageable.getPageNumber(), pageSize, pageable.getSort());
    }
}
