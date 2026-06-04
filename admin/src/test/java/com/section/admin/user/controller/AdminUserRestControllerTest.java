package com.section.admin.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.admin.user.req.AdminUserListRequest;
import com.section.admin.user.req.AdminUserSaveRequest;
import com.section.admin.user.res.AdminUserListResponse;
import com.section.admin.user.service.AdminUserService;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserRestControllerTest {

    @Mock
    private AdminUserService adminUserService;
    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private org.springframework.test.web.servlet.MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new AdminUserRestController(adminUserService, adminOperationPolicyService))
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("관리자 목록 API는 운영용 목록 응답을 반환한다")
    void getListReturnsAdminList() throws Exception {
        when(adminUserService.getAdminList(
                ArgumentMatchers.any(AdminUserListRequest.class),
                ArgumentMatchers.eq(0),
                ArgumentMatchers.eq(5)
        )).thenReturn(new AdminUserListResponse(
                List.of(new AdminUserListResponse.Item(1L, "master", "관리자", "ROLE_SUPER", "최고 관리자", "ACTIVE", "활성", "2026-06-04 09:00", "2026-05-01 08:00")),
                new AdminUserListResponse.Summary(1, 1, 0, 1, 0),
                0,
                5,
                1,
                1,
                1,
                1,
                new AdminUserListResponse.AppliedQuery("master", null, null, null, null),
                new AdminUserListResponse.ResultMeta("검색 결과 1명", "1-1 / 1명 · 1페이지", 1, true, "권한 우선 · 최근 로그인순 · 검색=master")
        ));

        mockMvc.perform(get("/api/admin/users/list").param("keyword", "master").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].loginId").value("master"))
                .andExpect(jsonPath("$.summary.superCount").value(1));
    }

    @Test
    @DisplayName("관리자 export API는 CSV 파일 응답을 반환한다")
    void exportReturnsCsvAttachment() throws Exception {
        when(adminUserService.exportAdminListCsv(ArgumentMatchers.any(AdminUserListRequest.class)))
                .thenReturn("csv".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/admin/users/export").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"admin-users-")));
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

    @Test
    @DisplayName("관리자 삭제 API는 성공 응답을 반환한다")
    void deleteReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/users/delete").param("no", "1"))
                .andExpect(status().isOk());
    }
}
