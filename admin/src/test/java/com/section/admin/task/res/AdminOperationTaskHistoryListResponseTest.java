package com.section.admin.task.res;

import com.section.admin.log.res.AdminLogListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminOperationTaskHistoryListResponseTest {

    @Test
    @DisplayName("운영 작업 이력 응답은 댓글 수정과 일괄 삭제 라벨을 한글로 노출한다")
    void fromMapsBulkDeleteAndCommentUpdateLabels() {
        AdminLogListResponse response = new AdminLogListResponse(
                List.of(
                        new AdminLogListResponse.Item(1L, 2L, "운영자", "TASK_COMMENT_UPDATE", 11L, "운영 작업 #11", "", "127.0.0.1", "2026-06-03 10:00"),
                        new AdminLogListResponse.Item(2L, 2L, "운영자", "TASK_BULK_DELETE", 12L, "운영 작업 #12", "", "127.0.0.1", "2026-06-03 10:05")
                ),
                2L,
                1,
                0,
                20,
                1L,
                2L,
                "1-2 / 2건 · 1페이지",
                new AdminLogListResponse.Summary(2, 0, 0, 0, 0, 0),
                new AdminLogListResponse.AppliedQuery(null, "TASK_", null, null, null),
                new AdminLogListResponse.ResultMeta("검색 결과 2건", "1-2 / 2건 · 1페이지", 1, "1-2 · 작업=TASK_")
        );

        AdminOperationTaskHistoryListResponse result = AdminOperationTaskHistoryListResponse.from(response, "/admin/settings/tasks");

        assertEquals("댓글 수정", result.items().get(0).actionLabel());
        assertEquals("일괄 삭제", result.items().get(1).actionLabel());
        assertEquals("/admin/settings/tasks?taskNo=11&openTaskNo=11&focusTaskNo=11&returnTo=%2Fadmin%2Fsettings%2Ftasks&source=task-history", result.items().get(0).taskPath());
    }
}
