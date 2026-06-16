package com.section.admin.product.support;

import com.section.admin.product.res.ProductFrontDisplayListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductFrontDisplayExportCsvWriterTest {

    @Test
    @DisplayName("프론트 노출 CSV 내보내기는 요약과 한글 필드를 포함한다")
    void writeIncludesSummaryAndDisplayFields() {
        ProductFrontDisplayListResponse item = new ProductFrontDisplayListResponse(
                4L,
                "990v6 Grey Day",
                "New Balance",
                "러닝화",
                289000,
                18L,
                "ACTIVE",
                "판매중",
                true,
                true,
                "Grey precision",
                "전시 설명",
                "Sharp tone",
                true,
                3
        );
        ProductFrontDisplayExportSummary summary = new ProductFrontDisplayExportSummary(
                "2026.06.07 10:15",
                "Featured 우선",
                "브랜드: New Balance | 노출 설정: 설정됨"
        );

        byte[] bytes = ProductFrontDisplayExportCsvWriter.write(summary, List.of(item));
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF);
        assertTrue(csv.contains("\"정렬\",\"Featured 우선\""));
        assertTrue(csv.contains("\"조회조건\",\"브랜드: New Balance | 노출 설정: 설정됨\""));
        assertTrue(csv.contains("상품번호,상품명,브랜드,카테고리,발매가,총재고,상태,노출설정,Featured,노출순서,헤드라인,무드,설명"));
        assertTrue(csv.contains("\"설정됨\""));
        assertTrue(csv.contains("\"판매중\""));
        assertTrue(csv.contains("\"289,000원\""));
        assertTrue(csv.contains("\"Grey precision\""));
    }
}
