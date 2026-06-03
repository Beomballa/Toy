package com.section.admin.task.req;

import com.section.common.base.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminOperationTaskBulkDuplicateRequestTest {

    @Test
    @DisplayName("운영 작업 일괄 복제 요청은 작업 번호를 중복 제거한다")
    void normalizedTaskNosDeduplicatesIds() {
        AdminOperationTaskBulkDuplicateRequest request =
                new AdminOperationTaskBulkDuplicateRequest(List.of(1L, 1L, 2L));

        assertEquals(List.of(1L, 2L), request.normalizedTaskNos());
    }

    @Test
    @DisplayName("운영 작업 일괄 복제 요청은 잘못된 작업 번호를 거부한다")
    void normalizedTaskNosRejectsInvalidIds() {
        AdminOperationTaskBulkDuplicateRequest request =
                new AdminOperationTaskBulkDuplicateRequest(List.of(0L, 2L));

        assertThrows(BusinessException.class, request::normalizedTaskNos);
    }
}
