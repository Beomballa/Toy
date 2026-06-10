package com.section.admin.settings.res;

import com.section.common.system.dto.AdminSystemSettingHistoryListQuery;
import com.section.common.system.dto.AdminSystemSettingHistoryListResDto;
import com.section.common.system.dto.AdminSystemSettingHistorySummaryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminSystemSettingHistoryListResponseTest {

    @Test
    @DisplayName("설정 변경 이력 페이지 라벨은 현재 페이지가 아닌 전체 페이지 수를 사용한다")
    void fromUsesTotalPageCount() {
        AdminSystemSettingHistoryListResDto row = new AdminSystemSettingHistoryListResDto();
        row.setHistoryNo(1L);
        row.setSettingKey("SYSTEM_MAINTENANCE_MODE");
        row.setSettingName("유지보수 모드");
        row.setBeforeValue("false");
        row.setAfterValue("true");
        row.setChangeSummary("유지보수 모드가 비활성에서 활성으로 변경되었습니다.");
        row.setChangedIpAddress("127.0.0.1");
        row.setCrtNo(7L);
        row.setCrtDtm(LocalDateTime.of(2026, 6, 6, 9, 0));

        AdminSystemSettingHistoryListResponse response = AdminSystemSettingHistoryListResponse.from(
                new PageImpl<>(List.of(row), PageRequest.of(1, 10), 21),
                Map.of(7L, "운영자"),
                new AdminSystemSettingHistoryListQuery(null, null, null, null, null),
                new AdminSystemSettingHistorySummaryDto(21L, 1L, 1L, 0L, 0L, 0L)
        );

        assertEquals("11-11 / 21건 · 3페이지", response.pageInfoLabel());
        assertEquals("11-11 / 21건 · 3페이지", response.resultMeta().pageInfoLabel());
    }

    @Test
    @DisplayName("설정 변경 이력 작업자명이 비어 있으면 기본 관리자명으로 대체한다")
    void fromFallsBackWhenAdminNameMissing() {
        AdminSystemSettingHistoryListResDto row = new AdminSystemSettingHistoryListResDto();
        row.setHistoryNo(2L);
        row.setSettingKey("ORDER_EXPORT_ENABLED");
        row.setSettingName("주문 Export 허용");
        row.setBeforeValue("true");
        row.setAfterValue("false");
        row.setChangeSummary("주문 Export 허용이 활성에서 비활성으로 변경되었습니다.");
        row.setChangedIpAddress("127.0.0.1");
        row.setCrtDtm(LocalDateTime.of(2026, 6, 6, 10, 0));

        AdminSystemSettingHistoryListResponse response = AdminSystemSettingHistoryListResponse.from(
                new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1),
                Map.of(),
                new AdminSystemSettingHistoryListQuery(null, null, null, null, null),
                new AdminSystemSettingHistorySummaryDto(1L, 1L, 0L, 0L, 1L, 0L)
        );

        assertEquals("관리자", response.items().getFirst().changedAdminName());
    }
}
