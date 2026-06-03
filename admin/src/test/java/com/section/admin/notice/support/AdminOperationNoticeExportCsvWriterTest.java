package com.section.admin.notice.support;

import com.section.common.system.dto.AdminOperationNoticeListResDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminOperationNoticeExportCsvWriterTest {

    @Test
    @DisplayName("운영 공지 CSV 내보내기는 BOM과 노출 상태 한글 라벨을 포함한다")
    void writeIncludesBomAndVisibilityLabel() {
        AdminOperationNoticeListResDto item = new AdminOperationNoticeListResDto();
        item.setNoticeNo(3L);
        item.setTitle("정기 점검");
        item.setContent("새벽 점검 안내와 사전 공지");
        item.setIsActive("Y");
        item.setIsPinned("Y");
        item.setStartDtm(LocalDateTime.of(2026, 6, 1, 9, 0));
        item.setEndDtm(LocalDateTime.of(2026, 6, 2, 9, 0));
        item.setCrtDtm(LocalDateTime.of(2026, 5, 31, 18, 30));

        byte[] bytes = AdminOperationNoticeExportCsvWriter.write(
                new AdminOperationNoticeExportSummary("2026.06.01 12:00", "고정 우선 · 최신 등록 순", "상태: 활성 | 고정: 고정"),
                List.of(item),
                LocalDateTime.of(2026, 6, 1, 12, 0)
        );
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF);
        assertTrue(csv.contains("\"조회조건\",\"상태: 활성 | 고정: 고정\""));
        assertTrue(csv.contains("공지번호,제목,내용요약,활성여부,고정여부,노출상태,노출시작,노출종료,등록일시"));
        assertTrue(csv.contains("\"정기 점검\""));
        assertTrue(csv.contains("\"노출중\""));
    }
}
