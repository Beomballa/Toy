package com.section.admin.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.admin.user.req.AdminMemberStatusUpdateRequest;
import com.section.admin.user.res.AdminMemberDetailResponse;
import com.section.admin.user.res.AdminMemberListResponse;
import com.section.admin.user.service.AdminMemberService;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminMemberRestControllerTest {

    @Mock
    private AdminMemberService adminMemberService;
    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminMemberRestController(adminMemberService, adminOperationPolicyService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("회원 목록 API는 운영용 페이지 응답을 반환한다")
    void getListReturnsPagedResponse() throws Exception {
        when(adminMemberService.getMemberList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminMemberListResponse(
                        List.of(new AdminMemberListResponse.Item(1L, "member@test.com", "회원", "닉네임", "N", "N", "N", "-")),
                        0, 20,
                        1L, 1, 1L, 1L,
                        new AdminMemberListResponse.AppliedQuery(null, null, null),
                        new AdminMemberListResponse.ResultMeta("전체 1명", "1-1 / 1명 · 1페이지", 0, false, "최신 가입순")
                ));

        mockMvc.perform(get("/api/admin/members/list?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("member@test.com"))
                .andExpect(jsonPath("$.totalElements").value(1L))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("전체 1명"));
    }

    @Test
    @DisplayName("회원 CSV 내보내기 API는 첨부 헤더와 본문을 반환한다")
    void exportReturnsAttachmentResponse() throws Exception {
        when(adminMemberService.exportMemberListCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn("csv".getBytes());

        mockMvc.perform(get("/api/admin/members/export").param("keyword", "member"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("members-")));

        verify(adminMemberService).exportMemberListCsv(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("회원 상세 API는 상세 응답을 반환한다")
    void getDetailReturnsDetailResponse() throws Exception {
        when(adminMemberService.getMemberDetail(3L))
                .thenReturn(new AdminMemberDetailResponse(3L, "user@test.com", "사용자", "유저", "Y", "N", "N", null, "-"));

        mockMvc.perform(get("/api/admin/members/get?id=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.masterYn").value("Y"));
    }

    @Test
    @DisplayName("회원 상태 변경 중 ACCOUNT_NOT_FOUND 예외는 404로 변환된다")
    void updateStatusReturnsNotFoundWhenMemberMissing() throws Exception {
        doThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND))
                .when(adminMemberService).updateMemberStatus(org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(patch("/api/admin/members/status/9")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AdminMemberStatusUpdateRequest(true, false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("M001"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 회원 상태 변경이 차단된다")
    void updateStatusReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(patch("/api/admin/members/status/9")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AdminMemberStatusUpdateRequest(true, false))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }
}
