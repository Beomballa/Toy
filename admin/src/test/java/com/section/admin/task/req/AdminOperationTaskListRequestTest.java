package com.section.admin.task.req;

import com.section.common.base.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminOperationTaskListRequestTest {

    @Test
    @DisplayName("운영 작업 목록 요청은 기한 시작일과 종료일을 정규화한다")
    void toQueryIncludesDueDateRange() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setDueDateFrom(LocalDate.of(2026, 6, 1));
        request.setDueDateTo(LocalDate.of(2026, 6, 30));

        var query = request.toQuery();

        assertEquals(LocalDate.of(2026, 6, 1), query.dueDateFrom());
        assertEquals(LocalDate.of(2026, 6, 30), query.dueDateTo());
    }

    @Test
    @DisplayName("운영 작업 목록 요청은 역전된 기한 범위를 거부한다")
    void toQueryRejectsInvalidDueDateRange() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setDueDateFrom(LocalDate.of(2026, 6, 30));
        request.setDueDateTo(LocalDate.of(2026, 6, 1));

        assertThrows(BusinessException.class, request::toQuery);
    }

    @Test
    @DisplayName("운영 작업 목록 요청은 메모 필터와 정렬 조건을 정규화한다")
    void toQueryNormalizesCommentedOnlyAndSortBy() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setCommentedOnly("y");
        request.setSortBy("latest_comment_desc");

        var query = request.toQuery();

        assertEquals("Y", query.commentedOnly());
        assertEquals("LATEST_COMMENT_DESC", query.sortBy());
    }

    @Test
    @DisplayName("운영 작업 목록 요청은 기한 상태 필터를 정규화한다")
    void toQueryNormalizesDueState() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setDueState("today");

        var query = request.toQuery();

        assertEquals("TODAY", query.dueState());
    }

    @Test
    @DisplayName("운영 작업 목록 요청은 잘못된 기한 상태를 거부한다")
    void toQueryRejectsInvalidDueState() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setDueState("LATER");

        assertThrows(BusinessException.class, request::toQuery);
    }
}
