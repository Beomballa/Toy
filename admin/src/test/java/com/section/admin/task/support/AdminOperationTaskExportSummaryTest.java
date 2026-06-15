package com.section.admin.task.support;

import com.section.common.system.dto.AdminOperationTaskListQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminOperationTaskExportSummaryTest {

    @Test
    @DisplayName("운영 작업 export 요약은 메모 상태 임박 마감 메모 수 정렬을 반영한다")
    void fromIncludesCommentStateDueWithinDaysAndCommentCountSort() {
        AdminOperationTaskExportSummary summary = AdminOperationTaskExportSummary.from(
                new AdminOperationTaskListQuery(
                        "정산",
                        31L,
                        "TODO",
                        "HIGH",
                        2L,
                        null,
                        null,
                        null,
                        "N",
                        7,
                        "OVERDUE",
                        "COMMENT_COUNT_DESC",
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30)
                ),
                Map.of(2L, "운영자")
        );

        assertEquals("메모 많은 순", summary.sortLabel());
        assertEquals("검색어: 정산 | 작업번호: #31 | 상태: 대기 | 우선순위: 높음 | 담당자: 운영자 | 메모없는 작업만 | 7일 이내 마감 | 기한상태: 기한 초과 | 기한: 2026-06-01 ~ 2026-06-30", summary.filterSummary());
    }
}
