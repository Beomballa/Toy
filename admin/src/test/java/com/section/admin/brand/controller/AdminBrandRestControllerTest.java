package com.section.admin.brand.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.brand.req.BrandSaveRequest;
import com.section.admin.brand.req.BrandStatusUpdateRequest;
import com.section.admin.brand.res.BrandListResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
                .thenReturn(new BrandListResponse(
                        List.of(new BrandResponse(1L, "나이키", "NIKE", "https://example.com/logo.png", "Y")),
                        0,
                        1,
                        1L,
                        10,
                        new BrandListResponse.AppliedQuery(null, null),
                        new BrandListResponse.ResultMeta("전체 1건", "1-1 / 1건 · 1페이지", 0, false, "브랜드명 기준", 1L, 1L)
                ));

        mockMvc.perform(get("/api/admin/brands/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].nameKo").value("나이키"))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("전체 1건"))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1L));
    }

    @Test
    @DisplayName("브랜드 CSV 내보내기 API는 첨부 헤더와 본문을 반환한다")
    void exportReturnsAttachmentResponse() throws Exception {
        when(adminBrandService.exportBrandListCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn("csv".getBytes());

        mockMvc.perform(get("/api/admin/brands/export").param("keyword", "나이키"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("brands-")));

        verify(adminBrandService).exportBrandListCsv(org.mockito.ArgumentMatchers.any());
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

    @Test
    @DisplayName("브랜드 삭제 API는 성공 응답을 반환한다")
    void deleteReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/brands/delete").param("no", "1"))
                .andExpect(status().isOk());
    }
}
