package com.section.admin.settings.support;

import com.section.common.system.dto.AdminSystemSettingHistoryListQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminSystemSettingHistoryExportSummaryTest {

    @Test
    @DisplayName("설정 이력 export 요약은 관리자 검색어를 포함한다")
    void ofIncludesAdminKeyword() {
        AdminSystemSettingHistoryExportSummary summary = AdminSystemSettingHistoryExportSummary.of(
                new AdminSystemSettingHistoryListQuery("SYSTEM_MAINTENANCE_MODE", 7L, "운영 담당", null, null),
                LocalDateTime.of(2026, 6, 10, 9, 30)
        );

        assertEquals("최신 변경순 · 설정=유지보수 모드 · 관리자=7 · 관리자검색=운영 담당", summary.filterSummary());
    }
}
