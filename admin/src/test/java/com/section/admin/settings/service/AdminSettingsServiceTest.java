package com.section.admin.settings.service;

import com.section.admin.settings.req.AdminSystemSettingSaveRequest;
import com.section.admin.settings.req.AdminSystemSettingHistoryListRequest;
import com.section.admin.settings.res.AdminSystemSettingHistoryDetailResponse;
import com.section.admin.settings.res.AdminSystemSettingHistoryListResponse;
import com.section.admin.settings.res.AdminSystemSettingResponse;
import com.section.common.system.dto.AdminSystemSettingHistoryListResDto;
import com.section.common.system.dto.AdminSystemSettingHistorySummaryDto;
import com.section.common.base.exception.BusinessException;
import com.section.common.system.entity.AdminSystemSetting;
import com.section.common.system.entity.AdminSystemSettingHistory;
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

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
    @DisplayName("시스템 설정 저장은 실제 변경된 항목만 일괄 upsert 한다")
    void saveSystemSettingsUpsertsAllKeys() {
        AdminSystemSettingSaveRequest request = new AdminSystemSettingSaveRequest(true, false, true, 30L);
        when(adminSystemSettingRepository.findAllBySettingKeyIn(any())).thenReturn(List.of());

        adminSettingsService.saveSystemSettings(request);

        verify(adminSystemSettingRepository, times(1)).saveAll(any());
        verify(adminSystemSettingRepository, never()).save(any(AdminSystemSetting.class));
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

        verify(adminSystemSettingRepository, times(0)).saveAll(any());
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
        when(adminSystemSettingHistoryRepository.getHistorySummary(any()))
                .thenReturn(new AdminSystemSettingHistorySummaryDto(1L, 1L, 1L, 0L, 0L, 0L, 1L, 0L));
        when(adminUserRepository.findAllById(List.of(7L)))
                .thenReturn(List.of(AdminUser.builder().adminNo(7L).name("운영자").build()));

        AdminSystemSettingHistoryListResponse response = adminSettingsService.getSystemSettingHistory(request, 0, 10);

        assertEquals(1, response.items().size());
        assertEquals("운영자", response.items().getFirst().changedAdminName());
        assertEquals("비활성", response.items().getFirst().beforeValueLabel());
        assertEquals("활성", response.items().getFirst().afterValueLabel());
        assertEquals("1-1 / 1건 · 1페이지", response.pageInfoLabel());
        assertEquals(1L, response.summary().totalCount());
    }

    @Test
    @DisplayName("설정 변경 이력 상세 조회는 관리자 이름과 표시값을 함께 응답한다")
    void getSystemSettingHistoryDetailReturnsResolvedAdminName() {
        AdminSystemSettingHistory history = AdminSystemSettingHistory.builder()
                .historyNo(11L)
                .settingKey("SYSTEM_MAINTENANCE_MODE")
                .settingName("유지보수 모드")
                .beforeValue("false")
                .afterValue("true")
                .changeSummary("유지보수 모드가 비활성에서 활성으로 변경되었습니다.")
                .changedIpAddress("127.0.0.1")
                .build();
        history.setCrtNo(7L);
        history.setCrtDtm(java.time.LocalDateTime.of(2026, 5, 29, 10, 0));

        when(adminSystemSettingHistoryRepository.findById(11L)).thenReturn(java.util.Optional.of(history));
        when(adminSystemSettingRepository.findAllBySettingKeyIn(List.of("SYSTEM_MAINTENANCE_MODE")))
                .thenReturn(List.of(AdminSystemSetting.builder()
                        .settingKey("SYSTEM_MAINTENANCE_MODE")
                        .settingValue("true")
                        .build()));
        when(adminUserRepository.findById(7L))
                .thenReturn(java.util.Optional.of(AdminUser.builder().adminNo(7L).name("운영자").build()));

        AdminSystemSettingHistoryDetailResponse response = adminSettingsService.getSystemSettingHistoryDetail(11L);

        assertEquals(11L, response.historyNo());
        assertEquals("운영자", response.changedAdminName());
        assertEquals("비활성", response.beforeValueLabel());
        assertEquals("활성", response.afterValueLabel());
        assertEquals("활성", response.currentValueLabel());
        assertTrue(response.currentValueMatched());
    }

    @Test
    @DisplayName("설정 변경 이력 상세 조회는 작업자 번호가 없으면 기본 관리자명을 사용한다")
    void getSystemSettingHistoryDetailFallsBackWhenAdminNoMissing() {
        AdminSystemSettingHistory history = AdminSystemSettingHistory.builder()
                .historyNo(15L)
                .settingKey("ORDER_EXPORT_ENABLED")
                .settingName("주문 Export 허용")
                .beforeValue("true")
                .afterValue("false")
                .changeSummary("주문 Export 허용이 활성에서 비활성으로 변경되었습니다.")
                .changedIpAddress("127.0.0.1")
                .build();
        history.setCrtDtm(java.time.LocalDateTime.of(2026, 6, 6, 15, 0));

        when(adminSystemSettingHistoryRepository.findById(15L)).thenReturn(java.util.Optional.of(history));
        when(adminSystemSettingRepository.findAllBySettingKeyIn(List.of("ORDER_EXPORT_ENABLED")))
                .thenReturn(List.of(AdminSystemSetting.builder()
                        .settingKey("ORDER_EXPORT_ENABLED")
                        .settingValue("true")
                        .build()));

        AdminSystemSettingHistoryDetailResponse response = adminSettingsService.getSystemSettingHistoryDetail(15L);

        assertEquals("관리자", response.changedAdminName());
        assertFalse(response.currentValueMatched());
    }

    @Test
    @DisplayName("설정 변경 이력 상세 조회는 현재 적용값과 다르면 과거 이력으로 표시한다")
    void getSystemSettingHistoryDetailMarksOutdatedHistory() {
        AdminSystemSettingHistory history = AdminSystemSettingHistory.builder()
                .historyNo(19L)
                .settingKey("LOW_STOCK_DEFAULT_THRESHOLD")
                .settingName("기본 저재고 임계값")
                .beforeValue("100")
                .afterValue("80")
                .changeSummary("기본 저재고 임계값이 100에서 80으로 변경되었습니다.")
                .changedIpAddress("127.0.0.1")
                .build();
        history.setCrtDtm(java.time.LocalDateTime.of(2026, 6, 6, 15, 0));

        when(adminSystemSettingHistoryRepository.findById(19L)).thenReturn(java.util.Optional.of(history));
        when(adminSystemSettingRepository.findAllBySettingKeyIn(List.of("LOW_STOCK_DEFAULT_THRESHOLD")))
                .thenReturn(List.of(AdminSystemSetting.builder()
                        .settingKey("LOW_STOCK_DEFAULT_THRESHOLD")
                        .settingValue("60")
                        .build()));

        AdminSystemSettingHistoryDetailResponse response = adminSettingsService.getSystemSettingHistoryDetail(19L);

        assertEquals("60", response.currentValue());
        assertEquals("60", response.currentValueLabel());
        assertFalse(response.currentValueMatched());
    }

    @Test
    @DisplayName("설정 변경 이력 CSV 내보내기는 표시값과 관리자명을 함께 포함한다")
    void exportSystemSettingHistoryCsvIncludesFormattedValues() {
        AdminSystemSettingHistoryListRequest request = new AdminSystemSettingHistoryListRequest();
        AdminSystemSettingHistoryListResDto row = new AdminSystemSettingHistoryListResDto();
        row.setHistoryNo(13L);
        row.setSettingKey("ORDER_EXPORT_ENABLED");
        row.setSettingName("주문 Export 허용");
        row.setBeforeValue("true");
        row.setAfterValue("false");
        row.setChangeSummary("주문 Export 허용이 활성에서 비활성으로 변경되었습니다.");
        row.setChangedIpAddress("127.0.0.1");
        row.setCrtNo(2L);
        row.setCrtDtm(java.time.LocalDateTime.of(2026, 6, 3, 12, 10));

        when(adminSystemSettingHistoryRepository.getHistoryList(any(), any()))
                .thenReturn(new PageImpl<>(List.of(row)));
        when(adminUserRepository.findAllById(List.of(2L)))
                .thenReturn(List.of(AdminUser.builder().adminNo(2L).name("설정담당").build()));

        byte[] result = adminSettingsService.exportSystemSettingHistoryCsv(request);
        String csv = new String(result, StandardCharsets.UTF_8);

        assertTrue(csv.contains("이력번호,변경시각,설정키,설정명,변경전(raw),변경후(raw),변경전(표시값),변경후(표시값),변경요약,관리자번호,관리자명,IP주소"));
        assertTrue(csv.contains("\"13\",\"2026-06-03 12:10\",\"ORDER_EXPORT_ENABLED\",\"주문 Export 허용\",\"true\",\"false\",\"활성\",\"비활성\",\"주문 Export 허용이 활성에서 비활성으로 변경되었습니다.\",\"2\",\"설정담당\",\"127.0.0.1\""));
    }
}
