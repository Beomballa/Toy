package com.section.admin.notice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.notice.req.AdminOperationNoticeBulkOperateRequest;
import com.section.admin.notice.req.AdminOperationNoticeSaveRequest;
import com.section.admin.notice.res.AdminOperationNoticeListResponse;
import com.section.admin.notice.service.AdminOperationNoticeService;
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
class AdminOperationNoticeRestControllerTest {

    @Mock
    private AdminOperationNoticeService adminOperationNoticeService;
    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOperationNoticeRestController(adminOperationNoticeService, adminOperationPolicyService))
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("운영 공지 목록 API는 운영형 페이지 응답을 반환한다")
    void getListReturnsPagedResponse() throws Exception {
        when(adminOperationNoticeService.getNoticeList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminOperationNoticeListResponse(
                        List.of(new AdminOperationNoticeListResponse.Item(1L, "점검 공지", "점검 안내", "Y", "Y", "노출중", "-", "-", "2026-05-19 10:00", "/admin/settings/logs?actionType=NOTICE_&targetId=1", "활동 로그")),
                        0,
                        1,
                        1L,
                        10,
                        new AdminOperationNoticeListResponse.NoticeStats(1L, 1L, 0L, 1L, "기본 문맥 기준", "고정 우선 최신순"),
                        new AdminOperationNoticeListResponse.AppliedQuery(null, null, null, null),
                        new AdminOperationNoticeListResponse.ResultMeta("전체 1건", "1-1 / 1건 · 1페이지", 0, false, "고정 우선 최신순", 1L, 1L)
                ));

        mockMvc.perform(get("/api/admin/settings/notices/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("점검 공지"))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("전체 1건"))
                .andExpect(jsonPath("$.noticeStats.liveCount").value(1L))
                .andExpect(jsonPath("$.items[0].activityLogPath").value("/admin/settings/logs?actionType=NOTICE_&targetId=1"));
    }

    @Test
    @DisplayName("운영 공지 상세 API는 단건 응답을 반환한다")
    void getDetailReturnsItem() throws Exception {
        when(adminOperationNoticeService.getNotice(1L))
                .thenReturn(com.section.common.system.entity.AdminOperationNotice.builder()
                        .noticeNo(1L)
                        .title("점검 공지")
                        .content("점검 안내")
                        .isActive("Y")
                        .isPinned("Y")
                        .build());

        mockMvc.perform(get("/api/admin/settings/notices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeNo").value(1L))
                .andExpect(jsonPath("$.title").value("점검 공지"))
                .andExpect(jsonPath("$.isActive").value("Y"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 운영 공지 저장이 차단된다")
    void saveReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        AdminOperationNoticeSaveRequest request = new AdminOperationNoticeSaveRequest(
                null,
                "점검 공지",
                "점검 안내",
                "Y",
                "N",
                null,
                null
        );
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(post("/api/admin/settings/notices/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("운영 공지 활성 상태 변경 API는 성공 응답을 반환한다")
    void updateActiveReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/admin/settings/notices/active/3?isActive=N"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"));
    }

    @Test
    @DisplayName("운영 공지 삭제 API는 성공 응답을 반환한다")
    void deleteReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/settings/notices/delete").param("no", "3"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("운영 공지 일괄 변경 API는 결과 응답을 반환한다")
    void bulkOperateReturnsResult() throws Exception {
        when(adminOperationNoticeService.bulkOperate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminOperationNoticeService.BulkOperateResult(3, 2, 1));

        mockMvc.perform(post("/api/admin/settings/notices/bulk-operate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AdminOperationNoticeBulkOperateRequest(List.of(1L, 2L, 3L), "Y", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(3))
                .andExpect(jsonPath("$.updatedCount").value(2))
                .andExpect(jsonPath("$.unchangedCount").value(1));
    }
}
