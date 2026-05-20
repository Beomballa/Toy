package com.section.admin.notice.req;

import com.section.common.base.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminOperationNoticeHistoryListRequestTest {

    @Test
    @DisplayName("운영 공지 이력 요청은 기본 actionType을 NOTICE_로 정규화한다")
    void toLogListRequestDefaultsToNoticePrefix() {
        AdminOperationNoticeHistoryListRequest request = new AdminOperationNoticeHistoryListRequest();
        request.setNoticeNo(3L);

        assertEquals("NOTICE_", request.toLogListRequest().getActionType());
        assertEquals(3L, request.toLogListRequest().getTargetId());
    }

    @Test
    @DisplayName("운영 공지 이력 요청은 허용되지 않은 actionType을 거부한다")
    void toLogListRequestRejectsInvalidActionType() {
        AdminOperationNoticeHistoryListRequest request = new AdminOperationNoticeHistoryListRequest();
        request.setActionType("PRODUCT_UPDATE");

        assertThrows(BusinessException.class, request::toLogListRequest);
    }

    @Test
    @DisplayName("운영 공지 이력 요청은 잘못된 날짜 범위를 거부한다")
    void toLogListRequestRejectsInvalidDateRange() {
        AdminOperationNoticeHistoryListRequest request = new AdminOperationNoticeHistoryListRequest();
        request.setStartDate(LocalDate.of(2026, 5, 20));
        request.setEndDate(LocalDate.of(2026, 5, 19));

        assertThrows(BusinessException.class, request::toLogListRequest);
    }
}
