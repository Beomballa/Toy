package com.section.admin.settings.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.system.dto.AdminSystemSettingHistoryListQuery;
import com.section.common.system.dto.AdminSystemSettingHistoryListResDto;
import com.section.common.system.dto.AdminSystemSettingHistorySummaryDto;
import com.section.common.system.entity.AdminSystemSetting;
import com.section.common.system.entity.AdminSystemSettingHistory;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminSystemSettingHistoryRepository;
import com.section.common.system.repository.AdminSystemSettingRepository;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class AdminSystemSettingHistoryRepositorySearchIntegrationTest {

    @Autowired
    private AdminSystemSettingHistoryRepository adminSystemSettingHistoryRepository;
    @Autowired
    private AdminSystemSettingRepository adminSystemSettingRepository;
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
                new AdminSystemSettingHistoryListQuery(null, null, null, null, LocalDate.now(), LocalDate.now())
        );

        assertEquals(4L, summary.totalCount());
        assertEquals(4L, summary.todayCount());
        assertEquals(1L, summary.maintenanceCount());
        assertEquals(1L, summary.communityCount());
        assertEquals(1L, summary.orderExportCount());
        assertEquals(1L, summary.lowStockThresholdCount());
    }

    @Test
    @DisplayName("설정 변경 이력 조회는 현재 반영중 여부와 과거 이력 필터를 함께 지원한다")
    void getHistoryListSupportsCurrentAndOutdatedFilters() {
        String settingKey = "TEST_SETTING_CURRENT_STATUS";
        AdminSystemSetting systemSetting = adminSystemSettingRepository.findBySettingKey(settingKey)
                .orElseGet(() -> adminSystemSettingRepository.save(AdminSystemSetting.builder()
                        .settingKey(settingKey)
                        .settingValue("8")
                        .description("테스트용 설정")
                        .build()));
        systemSetting.updateValue("70");
        adminSystemSettingHistoryRepository.save(history(settingKey, 999L, "100", "80"));
        adminSystemSettingHistoryRepository.save(history(settingKey, 999L, "80", "70"));

        List<AdminSystemSettingHistoryListResDto> currentItems = adminSystemSettingHistoryRepository.getHistoryList(
                new AdminSystemSettingHistoryListQuery(settingKey, null, null, "CURRENT", null, null),
                org.springframework.data.domain.PageRequest.of(0, 10)
        ).getContent();
        List<AdminSystemSettingHistoryListResDto> outdatedItems = adminSystemSettingHistoryRepository.getHistoryList(
                new AdminSystemSettingHistoryListQuery(settingKey, null, null, "OUTDATED", null, null),
                org.springframework.data.domain.PageRequest.of(0, 10)
        ).getContent();

        assertEquals(1, currentItems.size());
        assertEquals("70", currentItems.getFirst().getCurrentValue());
        assertEquals(true, currentItems.getFirst().getCurrentValueMatched());
        assertEquals(1, outdatedItems.size());
        assertEquals(false, outdatedItems.getFirst().getCurrentValueMatched());
    }

    private AdminSystemSettingHistory history(String key, Long adminNo) {
        return history(key, adminNo, "before", "after");
    }

    private AdminSystemSettingHistory history(String key, Long adminNo, String beforeValue, String afterValue) {
        AdminSystemSettingHistory history = AdminSystemSettingHistory.builder()
                .settingKey(key)
                .settingName(key)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .changeSummary("summary")
                .changedIpAddress("127.0.0.1")
                .build();
        history.setCrtNo(adminNo);
        history.setUptNo(adminNo);
        return history;
    }
}
