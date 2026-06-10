package com.section.admin.settings.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.system.dto.AdminSystemSettingHistoryListQuery;
import com.section.common.system.dto.AdminSystemSettingHistorySummaryDto;
import com.section.common.system.entity.AdminSystemSettingHistory;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminSystemSettingHistoryRepository;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class AdminSystemSettingHistoryRepositorySearchIntegrationTest {

    @Autowired
    private AdminSystemSettingHistoryRepository adminSystemSettingHistoryRepository;
    @Autowired
    private AdminUserRepository adminUserRepository;

    @Test
    @DisplayName("설정 변경 이력 요약은 단일 조건 집계로 전체 카운트를 정확히 반환한다")
    void getHistorySummaryAggregatesCountsAccurately() {
        adminSystemSettingHistoryRepository.save(history("SYSTEM_MAINTENANCE_MODE", 1L));
        adminSystemSettingHistoryRepository.save(history("COMMUNITY_WRITE_ENABLED", 2L));
        adminSystemSettingHistoryRepository.save(history("ORDER_EXPORT_ENABLED", 1L));
        adminSystemSettingHistoryRepository.save(history("LOW_STOCK_DEFAULT_THRESHOLD", 3L));

        AdminSystemSettingHistorySummaryDto summary = adminSystemSettingHistoryRepository.getHistorySummary(
                new AdminSystemSettingHistoryListQuery(null, null, null, LocalDate.now(), LocalDate.now())
        );

        assertEquals(4L, summary.totalCount());
        assertEquals(4L, summary.todayCount());
        assertEquals(1L, summary.maintenanceCount());
        assertEquals(1L, summary.communityCount());
        assertEquals(1L, summary.orderExportCount());
        assertEquals(1L, summary.lowStockThresholdCount());
    }

    private AdminSystemSettingHistory history(String key, Long adminNo) {
        AdminSystemSettingHistory history = AdminSystemSettingHistory.builder()
                .settingKey(key)
                .settingName(key)
                .beforeValue("before")
                .afterValue("after")
                .changeSummary("summary")
                .changedIpAddress("127.0.0.1")
                .build();
        history.setCrtNo(adminNo);
        history.setUptNo(adminNo);
        return history;
    }
}
