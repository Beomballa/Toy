package com.section.admin.brand.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.brand.req.BrandSaveRequest;
import com.section.admin.brand.req.BrandStatusUpdateRequest;
import com.section.admin.brand.res.BrandResponse;
import com.section.admin.brand.service.AdminBrandService;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminBrandRestControllerTest {

    @Mock
    private AdminBrandService adminBrandService;
    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminBrandRestController(adminBrandService, adminOperationPolicyService))
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("브랜드 목록 API는 운영용 목록 응답을 반환한다")
    void getListReturnsBrandList() throws Exception {
        when(adminBrandService.getBrandList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new BrandResponse(1L, "나이키", "NIKE", "https://example.com/logo.png", "Y")));

        mockMvc.perform(get("/api/admin/brands/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nameKo").value("나이키"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 브랜드 저장이 차단된다")
    void saveReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        BrandSaveRequest request = new BrandSaveRequest(null, "나이키", "NIKE", "https://example.com/logo.png", "Y");

        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(post("/api/admin/brands/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 브랜드 상태 변경이 차단된다")
    void updateActiveReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(patch("/api/admin/brands/active/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new BrandStatusUpdateRequest("N"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 브랜드 삭제가 차단된다")
    void deleteReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(delete("/api/admin/brands/delete").param("no", "1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }
}
