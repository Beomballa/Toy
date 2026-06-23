package com.section.admin.product.res;

import com.section.common.base.entity.type.ProductHistoryOrderType;
import com.section.common.commerce.dto.ProductHistoryListQuery;
import com.section.common.commerce.dto.ProductHistoryListResDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductHistoryListResponseTest {

    @Test
    @DisplayName("상품 이력 페이지 라벨은 현재 페이지가 아닌 전체 페이지 수를 사용한다")
    void ofUsesTotalPageCount() {
        ProductHistoryListResDto row = new ProductHistoryListResDto();
        row.setHistoryNo(1L);
        row.setProductNo(3L);
        row.setActionType("UPDATED");
        row.setSummary("상품 수정");
        row.setActionDtm(LocalDateTime.of(2026, 6, 6, 9, 0));

        ProductHistoryListResponse response = ProductHistoryListResponse.of(
                new PageImpl<>(List.of(row), PageRequest.of(1, 10), 21),
                new ProductHistoryListQuery(3L, null, null, null, null, null, null, ProductHistoryOrderType.LATEST)
        );

        assertEquals("11-11 / 21건 · 3페이지", response.pageInfoLabel());
        assertEquals("11-11 / 21건 · 3페이지", response.resultMeta().pageInfoLabel());
        assertEquals("검색 결과 21건", response.resultMeta().resultLabel());
    }
}
