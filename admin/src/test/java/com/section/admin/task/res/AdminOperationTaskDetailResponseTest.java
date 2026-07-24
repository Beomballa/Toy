package com.section.admin.task.res;

import com.section.admin.log.res.AdminLogListResponse;
import com.section.common.system.entity.AdminOperationTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminOperationTaskDetailResponseTest {

    @Test
    @DisplayName("운영 작업 최근 이력은 댓글 수정 액션을 한글 라벨로 노출한다")
    void fromMapsCommentUpdateActionLabel() {
        AdminOperationTask task = AdminOperationTask.builder()
                .taskNo(15L)
                .title("운영 점검")
                .description("설명")
                .status("TODO")
                .priority("HIGH")
                .isPinned("N")
                .sourceType("CONTENT_PERFORMANCE")
                .sourceId(31L)
                .build();
        AdminLogListResponse.Item history = new AdminLogListResponse.Item(
                3L,
                9L,
                "운영자",
                "TASK_COMMENT_UPDATE",
                15L,
                "",
                "",
                "127.0.0.1",
                "2026-06-01 10:00"
        );

        AdminOperationTaskDetailResponse response = AdminOperationTaskDetailResponse.from(
                task,
                "운영자",
                List.of(),
                List.of(),
                List.of(history),
                List.of()
        );

        assertEquals("댓글 수정", response.recentHistories().get(0).actionLabel());
        assertEquals("효과 분석 콘텐츠 #31", response.sourceLabel());
        assertEquals(
                "/admin/content/get?id=31&source=task-content-source&returnTo=%2Fadmin%2Fsettings%2Ftasks%2Fget%3Fno%3D15",
                response.sourcePath()
        );
    }
}
