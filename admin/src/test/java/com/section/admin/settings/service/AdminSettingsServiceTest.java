package com.section.admin.settings.service;

import com.section.admin.settings.req.AdminSystemSettingSaveRequest;
import com.section.admin.settings.res.AdminSystemSettingResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.system.entity.AdminSystemSetting;
import com.section.common.system.repository.AdminSystemSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
    @DisplayName("시스템 설정 저장은 4개 항목을 upsert 한다")
    void saveSystemSettingsUpsertsAllKeys() {
        AdminSystemSettingSaveRequest request = new AdminSystemSettingSaveRequest(true, false, true, 30L);
        when(adminSystemSettingRepository.findBySettingKey(any())).thenReturn(Optional.empty());

        adminSettingsService.saveSystemSettings(request);

        verify(adminSystemSettingRepository, times(4)).save(any(AdminSystemSetting.class));
    }

    @Test
    @DisplayName("저재고 기본 임계값이 0 이하이면 INVALID_INPUT_VALUE 예외를 던진다")
    void saveSystemSettingsRejectsNonPositiveThreshold() {
        AdminSystemSettingSaveRequest request = new AdminSystemSettingSaveRequest(true, true, true, 0L);

        assertThrows(BusinessException.class, () -> adminSettingsService.saveSystemSettings(request));
    }
}
