package com.section.admin.settings.service;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.entity.AdminSystemSetting;
import com.section.common.system.repository.AdminSystemSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationPolicyServiceTest {

    @Mock
    private AdminSystemSettingRepository adminSystemSettingRepository;

    @InjectMocks
    private AdminOperationPolicyService adminOperationPolicyService;

    @Test
    @DisplayName("유지보수 모드가 켜져 있으면 관리자 쓰기 작업을 막는다")
    void assertAdminWriteAllowedThrowsWhenMaintenanceEnabled() {
        when(adminSystemSettingRepository.findAllBySettingKeyIn(anyList()))
                .thenReturn(List.of(AdminSystemSetting.builder()
                        .settingKey(AdminOperationPolicyService.KEY_MAINTENANCE_MODE)
                        .settingValue("true")
                        .description("유지보수")
                        .build()));

        BusinessException exception = assertThrows(BusinessException.class, adminOperationPolicyService::assertAdminWriteAllowed);

        assertEquals(ErrorCode.ADMIN_MAINTENANCE_MODE, exception.getErrorCode());
    }

    @Test
    @DisplayName("커뮤니티 쓰기 설정이 꺼져 있으면 저장 작업을 막는다")
    void assertCommunityWriteAllowedThrowsWhenFeatureDisabled() {
        when(adminSystemSettingRepository.findAllBySettingKeyIn(anyList()))
                .thenReturn(List.of(AdminSystemSetting.builder()
                        .settingKey(AdminOperationPolicyService.KEY_COMMUNITY_WRITE)
                        .settingValue("false")
                        .description("커뮤니티 작성")
                        .build()));

        BusinessException exception = assertThrows(BusinessException.class, adminOperationPolicyService::assertCommunityWriteAllowed);

        assertEquals(ErrorCode.ADMIN_FEATURE_DISABLED, exception.getErrorCode());
    }

    @Test
    @DisplayName("주문 export 설정이 켜져 있으면 다운로드 작업을 허용한다")
    void assertOrderExportAllowedPassesWhenEnabled() {
        when(adminSystemSettingRepository.findAllBySettingKeyIn(anyList()))
                .thenReturn(List.of(AdminSystemSetting.builder()
                        .settingKey(AdminOperationPolicyService.KEY_ORDER_EXPORT)
                        .settingValue("true")
                        .description("주문 export")
                        .build()));

        assertDoesNotThrow(() -> adminOperationPolicyService.assertOrderExportAllowed());
    }
}
