package com.section.admin.notice.req;

import com.section.common.base.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminOperationNoticeBulkOperateRequestTest {

    @Test
    @DisplayName("일괄 운영 요청은 대상 번호를 중복 없이 정규화한다")
    void normalizedNoticeNosDeduplicatesValues() {
        AdminOperationNoticeBulkOperateRequest request = new AdminOperationNoticeBulkOperateRequest(
                List.of(3L, 3L, 5L),
                "Y",
                null
        );

        assertEquals(List.of(3L, 5L), request.normalizedNoticeNos());
    }

    @Test
    @DisplayName("일괄 운영 요청은 변경 항목이 없으면 거부한다")
    void validateOperationRejectsMissingChanges() {
        AdminOperationNoticeBulkOperateRequest request = new AdminOperationNoticeBulkOperateRequest(
                List.of(3L),
                null,
                null
        );

        assertThrows(BusinessException.class, request::validateOperation);
    }
}
