package com.section.admin.task.req;

import com.section.common.base.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminOperationTaskWorkloadListRequestTest {

    @Test
    @DisplayName("운영 작업 워크로드 요청은 검색 조건을 정규화한다")
    void toQueryNormalizesValues() {
        AdminOperationTaskWorkloadListRequest request = new AdminOperationTaskWorkloadListRequest();
        request.setKeyword("  운영   작업  ");
        request.setPriority("high");
        request.setOverdueOnly("y");

        var query = request.toQuery();

        assertEquals("운영 작업", query.keyword());
        assertEquals("HIGH", query.priority());
        assertEquals("Y", query.overdueOnly());
    }

    @Test
    @DisplayName("운영 작업 워크로드 요청은 잘못된 우선순위를 거부한다")
    void toQueryRejectsInvalidPriority() {
        AdminOperationTaskWorkloadListRequest request = new AdminOperationTaskWorkloadListRequest();
        request.setPriority("WRONG");

        assertThrows(BusinessException.class, request::toQuery);
    }
}
