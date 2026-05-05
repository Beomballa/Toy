package com.section.admin.product.support;

import com.section.common.commerce.dto.ProductListResDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductExportCsvWriterTest {

    @Test
    @DisplayName("상품 CSV 내보내기는 BOM과 한글 상태명을 포함한다")
    void writeIncludesBomAndStatusDesc() {
        ProductListResDto dto = new ProductListResDto();
        dto.setProductNo(1L);
        dto.setProductName("젤 카야노 14");
        dto.setProductModel("1201A019");
        dto.setBrandName("아식스");
        dto.setReleasePrice(129000);
        dto.setTotalStock(12L);
        dto.setStatus("SOLD_OUT");
        dto.setCrtDtm(LocalDateTime.of(2026, 5, 2, 13, 0));

        ProductExportSummary summary = new ProductExportSummary(
                "2026.05.05 12:30",
                "재고순",
                "브랜드: 아식스 | 검색어: 젤 카야노"
        );

        byte[] bytes = ProductExportCsvWriter.write(summary, List.of(dto));
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF);
        assertTrue(csv.contains("\"내보낸시각\",\"2026.05.05 12:30\""));
        assertTrue(csv.contains("\"정렬\",\"재고순\""));
        assertTrue(csv.contains("\"조회조건\",\"브랜드: 아식스 | 검색어: 젤 카야노\""));
        assertTrue(csv.contains("상품번호,상품명,모델번호,브랜드,발매가,총재고,상태,등록일시"));
        assertTrue(csv.contains("\"품절\""));
        assertTrue(csv.contains("\"129,000원\""));
    }
}
