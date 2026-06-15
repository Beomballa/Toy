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
        request.setCommentedOnly("n");
        request.setSortBy("comment_count_desc");
        request.setDueWithinDays(7);

        var query = request.toQuery();

        assertEquals("N", query.commentedOnly());
        assertEquals(7, query.dueWithinDays());
        assertEquals("COMMENT_COUNT_DESC", query.sortBy());
    }

    @Test
    @DisplayName("운영 작업 목록 요청은 작업 번호를 정확 검색 조건으로 정규화한다")
    void toQueryIncludesTaskNo() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setTaskNo(123L);

        var query = request.toQuery();

        assertEquals(123L, query.taskNo());
    }

    @Test
    @DisplayName("운영 작업 목록 요청은 음수 작업 번호를 거부한다")
    void toQueryRejectsNegativeTaskNo() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setTaskNo(-1L);

        assertThrows(BusinessException.class, request::toQuery);
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

    @Test
    @DisplayName("운영 작업 목록 요청은 30일을 초과한 임박 마감 범위를 거부한다")
    void toQueryRejectsInvalidDueWithinDays() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setDueWithinDays(31);

        assertThrows(BusinessException.class, request::toQuery);
    }
}
