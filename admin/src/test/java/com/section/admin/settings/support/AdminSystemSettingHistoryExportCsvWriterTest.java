package com.section.admin.settings.support;

import com.section.admin.settings.res.AdminSystemSettingHistoryListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSystemSettingHistoryExportCsvWriterTest {

    @Test
    @DisplayName("설정 변경 이력 CSV는 raw 값과 표시값을 함께 내보낸다")
    void writeProducesCsvWithReadableValues() {
        AdminSystemSettingHistoryListResponse.Item item = new AdminSystemSettingHistoryListResponse.Item(
                21L,
                "SYSTEM_MAINTENANCE_MODE",
                "유지보수 모드",
                "false",
                "true",
                "비활성",
                "활성",
                "유지보수 모드가 비활성에서 활성으로 변경되었습니다.",
                7L,
                "운영자",
                "127.0.0.1",
                "2026-06-03 12:25"
        );

        byte[] result = AdminSystemSettingHistoryExportCsvWriter.write(
                new AdminSystemSettingHistoryExportSummary("2026-06-03 12:30", "최신 변경순 · 설정=유지보수 모드"),
                List.of(item)
        );
        String csv = new String(result, StandardCharsets.UTF_8);

        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, new byte[]{result[0], result[1], result[2]});
        assertTrue(csv.contains("\"조회조건\",\"최신 변경순 · 설정=유지보수 모드\""));
        assertTrue(csv.contains("이력번호,변경시각,설정키,설정명,변경전(raw),변경후(raw),변경전(표시값),변경후(표시값),변경요약,관리자번호,관리자명,IP주소"));
        assertTrue(csv.contains("\"21\",\"2026-06-03 12:25\",\"SYSTEM_MAINTENANCE_MODE\",\"유지보수 모드\",\"false\",\"true\",\"비활성\",\"활성\",\"유지보수 모드가 비활성에서 활성으로 변경되었습니다.\",\"7\",\"운영자\",\"127.0.0.1\"\r\n"));
    }
}
