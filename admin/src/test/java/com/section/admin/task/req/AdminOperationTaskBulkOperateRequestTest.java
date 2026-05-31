package com.section.admin.task.req;

import com.section.common.base.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminOperationTaskBulkOperateRequestTest {

    @Test
    @DisplayName("운영 작업 일괄 변경 요청은 작업 번호를 중복 제거한다")
    void normalizedTaskNosDeduplicatesIds() {
        AdminOperationTaskBulkOperateRequest request =
                new AdminOperationTaskBulkOperateRequest(List.of(1L, 1L, 2L), "DONE", null, null, null, null);

        assertEquals(List.of(1L, 2L), request.normalizedTaskNos());
    }

    @Test
    @DisplayName("운영 작업 일괄 변경 요청은 변경 항목이 없으면 거부한다")
    void validateOperationRejectsEmptyChanges() {
        AdminOperationTaskBulkOperateRequest request =
                new AdminOperationTaskBulkOperateRequest(List.of(1L, 2L), null, null, null, null, null);

        assertThrows(BusinessException.class, request::validateOperation);
    }

    @Test
    @DisplayName("운영 작업 일괄 변경 요청은 잘못된 상태를 거부한다")
    void normalizedStatusRejectsInvalidValue() {
        AdminOperationTaskBulkOperateRequest request =
                new AdminOperationTaskBulkOperateRequest(List.of(1L), "WRONG", null, null, null, null);

        assertThrows(BusinessException.class, request::normalizedStatus);
    }

    @Test
    @DisplayName("운영 작업 일괄 변경 요청은 담당 해제를 유효한 변경으로 본다")
    void validateOperationAcceptsAssigneeClear() {
        AdminOperationTaskBulkOperateRequest request =
                new AdminOperationTaskBulkOperateRequest(List.of(1L), null, null, null, "clear", null);

        request.validateOperation();

        assertEquals("CLEAR", request.normalizedAssigneeMode());
    }
}
