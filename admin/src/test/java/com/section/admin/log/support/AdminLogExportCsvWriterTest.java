package com.section.admin.log.support;

import com.section.admin.log.res.AdminLogListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminLogExportCsvWriterTest {

    @Test
    @DisplayName("관리 활동 로그 CSV는 화면 기준 컬럼과 링크 정보를 유지한다")
    void writeProducesCsvWithMetadataAndRows() {
        AdminLogListResponse.Item item = new AdminLogListResponse.Item(
                15L,
                3L,
                "운영자",
                "PRODUCT_UPDATE",
                91L,
                "상품 #91",
                "/admin/products/history?productNo=91",
                "127.0.0.1",
                "2026-06-03 12:20"
        );

        byte[] result = AdminLogExportCsvWriter.write(
                new AdminLogExportSummary("2026-06-03 12:30", "최신순 · 관리자=3"),
                List.of(item)
        );
        String csv = new String(result, StandardCharsets.UTF_8);

        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, new byte[]{result[0], result[1], result[2]});
        assertTrue(csv.contains("\"내보낸시각\",\"2026-06-03 12:30\""));
        assertTrue(csv.contains("로그번호,관리자번호,관리자명,작업종류,대상ID,대상라벨,대상이동경로,IP주소,작업일시"));
        assertTrue(csv.contains("\"15\",\"3\",\"운영자\",\"PRODUCT_UPDATE\",\"91\",\"상품 #91\",\"/admin/products/history?productNo=91\",\"127.0.0.1\",\"2026-06-03 12:20\"\r\n"));
    }
}
