package com.section.admin.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.admin.user.req.AdminUserSaveRequest;
import com.section.admin.user.res.AdminUserListResponse;
import com.section.admin.user.service.AdminUserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserRestControllerTest {

    @Mock
    private AdminUserService adminUserService;
    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserRestController(adminUserService, adminOperationPolicyService))
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("관리자 목록 API는 운영용 목록 응답을 반환한다")
    void getListReturnsAdminList() throws Exception {
        when(adminUserService.getAdminList())
                .thenReturn(List.of(new AdminUserListResponse(1L, "master", "관리자", "ROLE_SUPER", "ACTIVE", null, null)));

        mockMvc.perform(get("/api/admin/users/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loginId").value("master"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 관리자 저장이 차단된다")
    void saveReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        AdminUserSaveRequest request = new AdminUserSaveRequest(null, "master", "1234", "관리자", "ROLE_SUPER", "ACTIVE");

        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(post("/api/admin/users/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 관리자 삭제가 차단된다")
    void deleteReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(delete("/api/admin/users/delete").param("no", "1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }
}
