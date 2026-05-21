package com.section.admin.task.req;

import com.section.common.base.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminOperationTaskListRequestTest {

    @Test
    @DisplayName("운영 작업 목록 요청은 검색 조건을 정규화한다")
    void toQueryNormalizesValues() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setKeyword("  운영   작업  ");
        request.setStatus("in_progress");
        request.setPriority("high");
        request.setAssigneeAdminNo(7L);

        var query = request.toQuery();

        assertEquals("운영 작업", query.keyword());
        assertEquals("IN_PROGRESS", query.status());
        assertEquals("HIGH", query.priority());
        assertEquals(7L, query.assigneeAdminNo());
    }

    @Test
    @DisplayName("운영 작업 목록 요청은 0 담당자를 null로 본다")
    void toQueryTreatsZeroAssigneeAsNull() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setAssigneeAdminNo(0L);

        assertNull(request.toQuery().assigneeAdminNo());
    }

    @Test
    @DisplayName("운영 작업 목록 요청은 잘못된 상태를 거부한다")
    void toQueryRejectsInvalidStatus() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setStatus("WRONG");

        assertThrows(BusinessException.class, request::toQuery);
    }
}
