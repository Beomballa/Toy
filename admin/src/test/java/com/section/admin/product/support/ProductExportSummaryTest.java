package com.section.admin.product.support;

import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.ProductListQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductExportSummaryTest {

    @Test
    @DisplayName("상품 export 요약은 추가 필터가 없으면 기본 문구를 반환한다")
    void fromReturnsDefaultSummaryWhenNoFiltersApplied() {
        ProductExportSummary summary = ProductExportSummary.from(
                new ProductListQuery(null, null, null, null, ProductOrderType.RECENT, false, null, false),
                null,
                null
        );

        assertEquals("최신순", summary.orderTypeLabel());
        assertEquals("추가 필터 없음", summary.filterSummary());
        assertTrue(summary.exportedAt().matches("\\d{4}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}"));
    }

    @Test
    @DisplayName("상품 export 요약은 현재 조회 조건을 사람이 읽을 수 있는 문구로 반환한다")
    void fromReturnsReadableFilterSummary() {
        ProductExportSummary summary = ProductExportSummary.from(
                new ProductListQuery(3L, 7L, ProductStatus.ACTIVE, "뉴발란스 993", ProductOrderType.STOCK_COUNT, true, 30L, true),
                "뉴발란스",
                "러닝화"
        );

        assertEquals("재고순", summary.orderTypeLabel());
        assertEquals(
                "브랜드: 뉴발란스 | 카테고리: 러닝화 | 상태: 판매중 | 저재고: 30개 미만 | 오늘 등록만 | 검색어: 뉴발란스 993",
                summary.filterSummary()
        );
    }
}
