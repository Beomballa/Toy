package com.section.admin.product.support;

import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.AdminFrontDisplayProductQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductFrontDisplayExportSummaryTest {

    @Test
    @DisplayName("프론트 노출 export 요약은 필터가 없으면 전체 상품 문구를 반환한다")
    void ofReturnsDefaultSummaryWhenNoFilterApplied() {
        ProductFrontDisplayExportSummary summary = ProductFrontDisplayExportSummary.of(
                new AdminFrontDisplayProductQuery(null, null, null, null, null, null, false, false, 20L, "FEATURED"),
                null,
                null
        );

        assertEquals("Featured 우선", summary.sortLabel());
        assertEquals("전체 상품", summary.filterSummary());
        assertTrue(summary.exportedAt().matches("\\d{4}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}"));
    }

    @Test
    @DisplayName("프론트 노출 export 요약은 현재 조회 조건을 읽기 쉬운 문구로 반환한다")
    void ofReturnsReadableFilterSummary() {
        ProductFrontDisplayExportSummary summary = ProductFrontDisplayExportSummary.of(
                new AdminFrontDisplayProductQuery("Grey", ProductStatus.ACTIVE, 7L, 11L, true, "READY", true, true, 30L, "PRICE_LOW"),
                "New Balance",
                "러닝화"
        );

        assertEquals("발매가 낮은 순", summary.sortLabel());
        assertEquals(
                "검색어: Grey | 상태: 판매중 | 브랜드: New Balance | 카테고리: 러닝화 | 노출 설정: 설정됨 | 전시 문구: 완성 | Featured만 | 저재고: 30개 미만",
                summary.filterSummary()
        );
    }
}
