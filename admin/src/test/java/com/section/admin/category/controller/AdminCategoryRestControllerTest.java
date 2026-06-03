package com.section.admin.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.category.req.CategorySaveRequest;
import com.section.admin.category.req.CategoryStatusUpdateRequest;
import com.section.admin.category.res.CategoryListResponse;
import com.section.admin.category.res.CategoryResponse;
import com.section.admin.category.service.AdminCategoryService;
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
class AdminCategoryRestControllerTest {

    @Mock
    private AdminCategoryService adminCategoryService;
    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCategoryRestController(adminCategoryService, adminOperationPolicyService))
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("카테고리 목록 API는 운영용 목록 응답을 반환한다")
    void getListReturnsCategoryList() throws Exception {
        when(adminCategoryService.getCategoryListByDepth(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CategoryListResponse(
                        List.of(new CategoryResponse(1L, null, "러닝화", 1, "Y")),
                        0,
                        1,
                        1L,
                        10,
                        new CategoryListResponse.AppliedQuery(null, null, 1),
                        new CategoryListResponse.ResultMeta("전체 1건", "1-1 / 1건 · 1페이지", 0, false, "대분류 기준", 1L, 1L)
                ));

        mockMvc.perform(get("/api/admin/categories/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("러닝화"))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("전체 1건"))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1L));
    }

    @Test
    @DisplayName("카테고리 CSV 내보내기 API는 첨부 헤더와 본문을 반환한다")
    void exportReturnsAttachmentResponse() throws Exception {
        when(adminCategoryService.exportCategoryListCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn("csv".getBytes());

        mockMvc.perform(get("/api/admin/categories/export").param("depth", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("categories-")));

        verify(adminCategoryService).exportCategoryListCsv(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("유지보수 모드에서는 카테고리 저장이 차단된다")
    void saveReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        CategorySaveRequest request = new CategorySaveRequest(null, null, "러닝화", 1, "Y");

        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(post("/api/admin/categories/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 카테고리 상태 변경이 차단된다")
    void updateActiveReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(patch("/api/admin/categories/active/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryStatusUpdateRequest("N"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 카테고리 삭제가 차단된다")
    void deleteReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(delete("/api/admin/categories/delete").param("no", "1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("카테고리 삭제 API는 성공 응답을 반환한다")
    void deleteReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/categories/delete").param("no", "1"))
                .andExpect(status().isOk());
    }
}
