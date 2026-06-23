package com.section.admin.product.support;

import com.section.common.base.entity.type.ProductHistoryActionType;
import com.section.common.base.entity.type.ProductHistoryOrderType;
import com.section.common.commerce.dto.ProductHistoryListQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductHistoryExportSummaryTest {

    @Test
    @DisplayName("상품 변경 이력 export 요약은 추가 필터가 없으면 기본 문구를 반환한다")
    void fromReturnsDefaultSummaryWhenNoFiltersApplied() {
        ProductHistoryExportSummary summary = ProductHistoryExportSummary.from(
                new ProductHistoryListQuery(null, null, null, null, null, null, null, ProductHistoryOrderType.LATEST)
        );

        assertEquals("최신순", summary.orderTypeLabel());
        assertEquals("추가 필터 없음", summary.filterSummary());
        assertTrue(summary.exportedAt().matches("\\d{4}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}"));
    }

    @Test
    @DisplayName("상품 변경 이력 export 요약은 현재 조회 조건을 사람이 읽을 수 있는 문구로 반환한다")
    void fromReturnsReadableFilterSummary() {
        ProductHistoryExportSummary summary = ProductHistoryExportSummary.from(
                new ProductHistoryListQuery(4L, ProductHistoryActionType.UPDATED, "썸네일", 9L, "관리자", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), ProductHistoryOrderType.OLDEST)
        );

        assertEquals("오래된순", summary.orderTypeLabel());
        assertEquals(
                "상품번호: 4 | 작업유형: 수정 | 요약검색: 썸네일 | 작업자번호: 9 | 작업자명: 관리자 | 기간: 2026-06-01 ~ 2026-06-30",
                summary.filterSummary()
        );
    }
}
