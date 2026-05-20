package com.section.admin.notice.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.system.dto.AdminOperationNoticeListQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminOperationNoticeListRequestTest {

    @Test
    @DisplayName("운영 공지 목록 요청은 검색/상태/고정값을 정규화한다")
    void toQueryNormalizesValues() {
        AdminOperationNoticeListRequest request = new AdminOperationNoticeListRequest();
        request.setKeyword("  점검  공지 ");
        request.setIsActive(" y ");
        request.setIsPinned(" n ");
        request.setVisibilityStatus(" live ");

        AdminOperationNoticeListQuery query = request.toQuery();

        assertEquals("점검 공지", query.keyword());
        assertEquals("Y", query.isActive());
        assertEquals("N", query.isPinned());
        assertEquals(com.section.common.base.entity.type.AdminNoticeVisibilityStatus.LIVE, query.visibilityStatus());
    }

    @Test
    @DisplayName("운영 공지 목록 요청은 잘못된 YN 값을 거부한다")
    void toQueryRejectsInvalidFlag() {
        AdminOperationNoticeListRequest request = new AdminOperationNoticeListRequest();
        request.setIsActive("MAYBE");

        assertThrows(BusinessException.class, request::toQuery);
    }

    @Test
    @DisplayName("운영 공지 목록 요청은 잘못된 노출 상태 값을 거부한다")
    void toQueryRejectsInvalidVisibilityStatus() {
        AdminOperationNoticeListRequest request = new AdminOperationNoticeListRequest();
        request.setVisibilityStatus("UNKNOWN");

        assertThrows(BusinessException.class, request::toQuery);
    }
}
