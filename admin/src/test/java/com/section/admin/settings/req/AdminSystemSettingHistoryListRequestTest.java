package com.section.admin.settings.req;

import com.section.common.base.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminSystemSettingHistoryListRequestTest {

    @Test
    @DisplayName("설정 이력 요청은 관리자 검색어 공백을 정규화한다")
    void toQueryNormalizesAdminKeyword() {
        AdminSystemSettingHistoryListRequest request = new AdminSystemSettingHistoryListRequest();
        request.setAdminKeyword("  설정   담당자  ");
        request.setChangeStatus(" current ");

        var query = request.toQuery();

        assertEquals("설정 담당자", query.adminKeyword());
        assertEquals("CURRENT", query.changeStatus());
    }

    @Test
    @DisplayName("설정 이력 요청은 역전된 기간을 거부한다")
    void toQueryRejectsInvalidDateRange() {
        AdminSystemSettingHistoryListRequest request = new AdminSystemSettingHistoryListRequest();
        request.setStartDate(LocalDate.of(2026, 6, 9));
        request.setEndDate(LocalDate.of(2026, 6, 1));

        assertThrows(BusinessException.class, request::toQuery);
    }

    @Test
    @DisplayName("설정 이력 요청은 허용되지 않은 이력 상태 값을 거부한다")
    void toQueryRejectsInvalidChangeStatus() {
        AdminSystemSettingHistoryListRequest request = new AdminSystemSettingHistoryListRequest();
        request.setChangeStatus("latest");

        assertThrows(BusinessException.class, request::toQuery);
    }
}
