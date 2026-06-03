package com.section.admin.settings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.settings.res.AdminSystemSettingHistoryDetailResponse;
import com.section.admin.settings.res.AdminSystemSettingHistoryListResponse;
import com.section.admin.settings.req.AdminSystemSettingSaveRequest;
import com.section.admin.settings.res.AdminSystemSettingResponse;
import com.section.admin.settings.service.AdminSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminSettingsRestControllerTest {

    @Mock
    private AdminSettingsService adminSettingsService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminSettingsRestController(adminSettingsService))
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("시스템 설정 조회 API는 현재 설정 값을 반환한다")
    void getSystemSettingsReturnsCurrentValues() throws Exception {
        when(adminSettingsService.getSystemSettings())
                .thenReturn(new AdminSystemSettingResponse(true, false, true, 30L));

        mockMvc.perform(get("/api/admin/settings/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceMode").value(true))
                .andExpect(jsonPath("$.communityWriteEnabled").value(false))
                .andExpect(jsonPath("$.orderExportEnabled").value(true))
                .andExpect(jsonPath("$.lowStockDefaultThreshold").value(30L));
    }

    @Test
    @DisplayName("시스템 설정 이력 조회 API는 최근 변경 내역을 반환한다")
    void getSystemSettingHistoryReturnsItems() throws Exception {
        when(adminSettingsService.getSystemSettingHistory(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(5)))
                .thenReturn(new AdminSystemSettingHistoryListResponse(
                        java.util.List.of(new AdminSystemSettingHistoryListResponse.Item(
                                1L,
                                "SYSTEM_MAINTENANCE_MODE",
                                "유지보수 모드",
                                "false",
                                "true",
                                "비활성",
                                "활성",
                                "유지보수 모드가 비활성에서 활성으로 변경되었습니다.",
                                9L,
                                "운영자",
                                "127.0.0.1",
                                "2026-05-28 10:00"
                        )),
                        1,
                        1,
                        0,
                        5,
                        1,
                        1,
                        "1-1 / 1건 · 1페이지",
                        new AdminSystemSettingHistoryListResponse.Summary(1, 1, 1, 0, 0, 0),
                        new AdminSystemSettingHistoryListResponse.AppliedQuery("SYSTEM_MAINTENANCE_MODE", 9L, "2026-05-28", "2026-05-28"),
                        new AdminSystemSettingHistoryListResponse.ResultMeta("1-1 / 1건", "1-1 / 1건 · 1페이지", 2, "최신 변경순 · 설정=유지보수 모드")
                ));

        mockMvc.perform(get("/api/admin/settings/system/history")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].settingKey").value("SYSTEM_MAINTENANCE_MODE"))
                .andExpect(jsonPath("$.items[0].changedAdminName").value("운영자"))
                .andExpect(jsonPath("$.summary.totalCount").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("시스템 설정 이력 export API는 CSV 다운로드 헤더를 반환한다")
    void exportSystemSettingHistoryReturnsCsvAttachment() throws Exception {
        when(adminSettingsService.exportSystemSettingHistoryCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn("이력번호\n1".getBytes());

        mockMvc.perform(get("/api/admin/settings/system/history/export").param("settingKey", "SYSTEM_MAINTENANCE_MODE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"system-setting-history-")))
                .andExpect(content().bytes("이력번호\n1".getBytes()));
    }

    @Test
    @DisplayName("시스템 설정 이력 상세 조회 API는 단건 상세 정보를 반환한다")
    void getSystemSettingHistoryDetailReturnsItem() throws Exception {
        when(adminSettingsService.getSystemSettingHistoryDetail(11L))
                .thenReturn(new AdminSystemSettingHistoryDetailResponse(
                        11L,
                        "SYSTEM_MAINTENANCE_MODE",
                        "유지보수 모드",
                        "false",
                        "true",
                        "비활성",
                        "활성",
                        "유지보수 모드가 비활성에서 활성으로 변경되었습니다.",
                        7L,
                        "운영자",
                        "127.0.0.1",
                        "2026-05-29 10:00:00"
                ));

        mockMvc.perform(get("/api/admin/settings/system/history/get").param("historyNo", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historyNo").value(11))
                .andExpect(jsonPath("$.settingKey").value("SYSTEM_MAINTENANCE_MODE"))
                .andExpect(jsonPath("$.changedAdminName").value("운영자"));
    }

    @Test
    @DisplayName("시스템 설정 저장 API는 잘못된 임계값 요청을 400으로 변환한다")
    void saveSystemSettingsReturnsBadRequestWhenThresholdInvalid() throws Exception {
        AdminSystemSettingSaveRequest request = new AdminSystemSettingSaveRequest(true, true, true, 0L);

        mockMvc.perform(post("/api/admin/settings/system")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }
}
