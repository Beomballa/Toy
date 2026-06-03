package com.section.admin.task.support;

import com.section.common.system.dto.AdminOperationTaskListResDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminOperationTaskExportCsvWriterTest {

    @Test
    @DisplayName("운영 작업 CSV 내보내기는 한글 상태명과 마감상태를 포함한다")
    void writeIncludesLocalizedLabels() {
        AdminOperationTaskListResDto item = new AdminOperationTaskListResDto();
        item.setTaskNo(7L);
        item.setTitle("정산 확인");
        item.setDescription("이번 주 마감 정리");
        item.setStatus("TODO");
        item.setPriority("HIGH");
        item.setAssigneeAdminName("운영자");
        item.setDueDate(LocalDate.of(2026, 6, 1));
        item.setIsPinned("Y");
        item.setCommentCount(2L);
        item.setLatestCommentContent("최근 메모");
        item.setLatestCommentAdminName("운영자");
        item.setLatestCommentDtm(LocalDateTime.of(2026, 6, 1, 9, 30));
        item.setCrtDtm(LocalDateTime.of(2026, 5, 31, 14, 0));

        byte[] bytes = AdminOperationTaskExportCsvWriter.write(
                new AdminOperationTaskExportSummary("2026.06.01 12:00", "고정 우선 · 마감 임박 순", "상태: 대기 | 담당자: 운영자"),
                List.of(item),
                LocalDate.of(2026, 6, 1)
        );
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF);
        assertTrue(csv.contains("\"조회조건\",\"상태: 대기 | 담당자: 운영자\""));
        assertTrue(csv.contains("작업번호,제목,설명,상태,우선순위,담당자,마감일,마감상태,고정여부,메모수,최근메모,최근메모작성자,최근메모일시,등록일시"));
        assertTrue(csv.contains("\"대기\",\"높음\",\"운영자\",\"2026-06-01\",\"오늘 마감\",\"고정\""));
        assertTrue(csv.contains("\"2\",\"최근 메모\",\"운영자\",\"2026-06-01 09:30\""));
    }
}
