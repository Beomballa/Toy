package com.section.admin.log.req;

import com.section.common.base.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminLogListRequestTest {

    @Test
    @DisplayName("활동 로그 요청은 관리자명과 작업 종류를 공백 정리해 query로 변환한다")
    void toQueryNormalizesAdminKeywordAndActionType() {
        AdminLogListRequest request = new AdminLogListRequest();
        request.setAdminKeyword("  정산   운영자 ");
        request.setActionType(" task_ ");

        var query = request.toQuery();

        assertEquals("정산 운영자", query.adminKeyword());
        assertEquals("task_", query.actionType());
    }

    @Test
    @DisplayName("활동 로그 요청은 역전된 날짜 범위를 거부한다")
    void toQueryRejectsInvalidDateRange() {
        AdminLogListRequest request = new AdminLogListRequest();
        request.setStartDate(LocalDate.of(2026, 6, 14));
        request.setEndDate(LocalDate.of(2026, 6, 1));

        assertThrows(BusinessException.class, request::toQuery);
    }
}
