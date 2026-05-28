package com.section.admin.settings.service;

import com.section.admin.settings.req.AdminSystemSettingSaveRequest;
import com.section.admin.settings.req.AdminSystemSettingHistoryListRequest;
import com.section.admin.settings.res.AdminSystemSettingHistoryListResponse;
import com.section.admin.settings.res.AdminSystemSettingResponse;
import com.section.common.system.dto.AdminSystemSettingHistoryListResDto;
import com.section.common.base.exception.BusinessException;
import com.section.common.system.entity.AdminSystemSetting;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminSystemSettingHistoryRepository;
import com.section.common.system.repository.AdminSystemSettingRepository;
import com.section.common.system.repository.AdminUserRepository;
import org.springframework.data.domain.PageImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSettingsServiceTest {

    @Mock
    private AdminSystemSettingRepository adminSystemSettingRepository;

    @Mock
    private AdminSystemSettingHistoryRepository adminSystemSettingHistoryRepository;

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminSettingsService adminSettingsService;

    @Test
    @DisplayName("시스템 설정은 저장된 값이 없으면 기본값을 반환한다")
    void getSystemSettingsReturnsDefaults() {
        when(adminSystemSettingRepository.findAllBySettingKeyIn(any())).thenReturn(List.of());

        AdminSystemSettingResponse response = adminSettingsService.getSystemSettings();

        assertFalse(response.maintenanceMode());
        assertTrue(response.communityWriteEnabled());
        assertTrue(response.orderExportEnabled());
        assertEquals(100L, response.lowStockDefaultThreshold());
    }

    @Test
    @DisplayName("시스템 설정 저장은 실제 변경된 항목만 upsert 한다")
    void saveSystemSettingsUpsertsAllKeys() {
        AdminSystemSettingSaveRequest request = new AdminSystemSettingSaveRequest(true, false, true, 30L);
        when(adminSystemSettingRepository.findAllBySettingKeyIn(any())).thenReturn(List.of());

        adminSettingsService.saveSystemSettings(request);

        verify(adminSystemSettingRepository, times(3)).save(any(AdminSystemSetting.class));
        verify(adminSystemSettingHistoryRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("저재고 기본 임계값이 0 이하이면 INVALID_INPUT_VALUE 예외를 던진다")
    void saveSystemSettingsRejectsNonPositiveThreshold() {
        AdminSystemSettingSaveRequest request = new AdminSystemSettingSaveRequest(true, true, true, 0L);

        assertThrows(BusinessException.class, () -> adminSettingsService.saveSystemSettings(request));
    }

    @Test
    @DisplayName("시스템 설정 저장은 기존 값과 동일하면 저장이나 이력을 남기지 않는다")
    void saveSystemSettingsSkipsUnchangedValues() {
        AdminSystemSettingSaveRequest request = new AdminSystemSettingSaveRequest(true, false, true, 30L);
        when(adminSystemSettingRepository.findAllBySettingKeyIn(any())).thenReturn(List.of(
                AdminSystemSetting.builder().settingKey("SYSTEM_MAINTENANCE_MODE").settingValue("true").build(),
                AdminSystemSetting.builder().settingKey("COMMUNITY_WRITE_ENABLED").settingValue("false").build(),
                AdminSystemSetting.builder().settingKey("ORDER_EXPORT_ENABLED").settingValue("true").build(),
                AdminSystemSetting.builder().settingKey("LOW_STOCK_DEFAULT_THRESHOLD").settingValue("30").build()
        ));

        adminSettingsService.saveSystemSettings(request);

        verify(adminSystemSettingRepository, times(0)).save(any(AdminSystemSetting.class));
        verify(adminSystemSettingHistoryRepository, times(0)).saveAll(any());
    }

    @Test
    @DisplayName("설정 변경 이력 조회는 관리자 이름을 함께 응답한다")
    void getSystemSettingHistoryReturnsAdminNames() {
        AdminSystemSettingHistoryListRequest request = new AdminSystemSettingHistoryListRequest();
        AdminSystemSettingHistoryListResDto row = new AdminSystemSettingHistoryListResDto();
        row.setHistoryNo(11L);
        row.setSettingKey("SYSTEM_MAINTENANCE_MODE");
        row.setSettingName("유지보수 모드");
        row.setBeforeValue("false");
        row.setAfterValue("true");
        row.setChangeSummary("유지보수 모드가 비활성에서 활성으로 변경되었습니다.");
        row.setChangedIpAddress("127.0.0.1");
        row.setCrtNo(7L);
        row.setCrtDtm(java.time.LocalDateTime.of(2026, 5, 28, 10, 0));

        when(adminSystemSettingHistoryRepository.getHistoryList(any(), any()))
                .thenReturn(new PageImpl<>(List.of(row)));
        when(adminUserRepository.findAllById(List.of(7L)))
                .thenReturn(List.of(AdminUser.builder().adminNo(7L).name("운영자").build()));

        AdminSystemSettingHistoryListResponse response = adminSettingsService.getSystemSettingHistory(request, 0, 10);

        assertEquals(1, response.items().size());
        assertEquals("운영자", response.items().getFirst().changedAdminName());
        assertEquals("비활성", response.items().getFirst().beforeValueLabel());
        assertEquals("활성", response.items().getFirst().afterValueLabel());
    }
}
