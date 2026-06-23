package com.section.admin.product.support;

import com.section.common.commerce.dto.ProductHistoryListResDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductHistoryExportCsvWriterTest {

    @Test
    @DisplayName("상품 변경 이력 CSV 내보내기는 BOM과 한글 작업명을 포함한다")
    void writeIncludesBomAndReadableActionLabel() {
        ProductHistoryListResDto dto = new ProductHistoryListResDto();
        dto.setHistoryNo(7L);
        dto.setProductNo(4L);
        dto.setActionType("UPDATED");
        dto.setSummary("썸네일이 변경되었습니다.");
        dto.setStatusSnapshot("ACTIVE");
        dto.setOptionCount(2);
        dto.setTotalStock(8L);
        dto.setActorNo(1L);
        dto.setActorName("관리자");
        dto.setActionDtm(LocalDateTime.of(2026, 6, 23, 21, 10));

        ProductHistoryExportSummary summary = new ProductHistoryExportSummary(
                "2026.06.23 21:30",
                "오래된순",
                "상품번호: 4 | 작업유형: 수정"
        );

        byte[] bytes = ProductHistoryExportCsvWriter.write(summary, List.of(dto));
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF);
        assertTrue(csv.contains("\"내보낸시각\",\"2026.06.23 21:30\""));
        assertTrue(csv.contains("\"정렬\",\"오래된순\""));
        assertTrue(csv.contains("\"조회조건\",\"상품번호: 4 | 작업유형: 수정\""));
        assertTrue(csv.contains("이력번호,상품번호,작업유형,요약,상태스냅샷,옵션수,총재고,작업자번호,작업자명,작업일시"));
        assertTrue(csv.contains("\"수정\""));
        assertTrue(csv.contains("\"8개\""));
    }
}
