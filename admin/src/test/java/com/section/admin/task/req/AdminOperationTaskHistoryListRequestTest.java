package com.section.admin.task.req;

import com.section.common.base.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminOperationTaskHistoryListRequestTest {

    @Test
    @DisplayName("운영 작업 이력 요청은 기본 actionType을 TASK_ 로 사용한다")
    void toLogListRequestUsesTaskPrefixByDefault() {
        AdminOperationTaskHistoryListRequest request = new AdminOperationTaskHistoryListRequest();
        request.setTaskNo(3L);

        var logRequest = request.toLogListRequest();

        assertEquals(3L, logRequest.getTargetId());
        assertEquals("TASK_", logRequest.getActionType());
    }

    @Test
    @DisplayName("운영 작업 이력 요청은 잘못된 작업 유형을 거부한다")
    void toLogListRequestRejectsInvalidActionType() {
        AdminOperationTaskHistoryListRequest request = new AdminOperationTaskHistoryListRequest();
        request.setActionType("WRONG");

        assertThrows(BusinessException.class, request::toLogListRequest);
    }
}
