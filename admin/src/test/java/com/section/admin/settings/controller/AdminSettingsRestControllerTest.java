package com.section.admin.settings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
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
