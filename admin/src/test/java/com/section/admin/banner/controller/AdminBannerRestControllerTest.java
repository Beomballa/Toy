package com.section.admin.banner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.banner.req.BannerSaveRequest;
import com.section.admin.banner.res.BannerListResponse;
import com.section.admin.banner.service.AdminBannerService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminBannerRestControllerTest {

    @Mock
    private AdminBannerService adminBannerService;
    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminBannerRestController(adminBannerService, adminOperationPolicyService))
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("배너 목록 API는 운영용 리스트 응답을 반환한다")
    void getListReturnsBannerResponse() throws Exception {
        when(adminBannerService.getBannerList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new BannerListResponse(
                        List.of(new BannerListResponse.Item(1L, "메인 배너", "https://example.com/banner.png", null, "2026-05-10 10:00", "2026-05-20 10:00", 1, "Y", "노출중")),
                        0,
                        1,
                        1L,
                        10,
                        new BannerListResponse.AppliedQuery(null, null, null),
                        new BannerListResponse.ResultMeta("전체 1건", "1-1 / 1건 · 1페이지", 0, false, "정렬 순서 기준", 1L, 1L)
                ));

        mockMvc.perform(get("/api/admin/banners/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("메인 배너"))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("전체 1건"))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1L));
    }

    @Test
    @DisplayName("배너 목록 API는 노출 기간 필터도 응답 계약에 반영한다")
    void getListIncludesExposureStatusFilter() throws Exception {
        when(adminBannerService.getBannerList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new BannerListResponse(
                        List.of(),
                        0,
                        0,
                        0L,
                        10,
                        new BannerListResponse.AppliedQuery(null, "Y", "LIVE"),
                        new BannerListResponse.ResultMeta("검색 결과 0건", "조건에 맞는 배너가 없습니다.", 2, true, "정렬 순서 기준 · 상태=사용 · 노출기간=진행중", 0L, 0L)
                ));

        mockMvc.perform(get("/api/admin/banners/list").param("isActive", "Y").param("exposureStatus", "LIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedQuery.isActive").value("Y"))
                .andExpect(jsonPath("$.appliedQuery.exposureStatus").value("LIVE"))
                .andExpect(jsonPath("$.resultMeta.querySignature").value("정렬 순서 기준 · 상태=사용 · 노출기간=진행중"));
    }

    @Test
    @DisplayName("배너 저장 API는 잘못된 요청을 400으로 변환한다")
    void saveReturnsBadRequestWhenInvalid() throws Exception {
        BannerSaveRequest request = new BannerSaveRequest(
                null,
                "배너",
                "https://example.com/banner.png",
                null,
                LocalDateTime.of(2026, 5, 11, 10, 0),
                LocalDateTime.of(2026, 5, 10, 10, 0),
                1,
                "Y"
        );

        doThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE))
                .when(adminBannerService).saveBanner(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/admin/banners/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("배너 상태 변경 API는 성공 응답을 반환한다")
    void updateActiveReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/admin/banners/active/3?isActive=N"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 배너 저장이 차단된다")
    void saveReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        BannerSaveRequest request = new BannerSaveRequest(
                null,
                "배너",
                "https://example.com/banner.png",
                null,
                LocalDateTime.of(2026, 5, 10, 10, 0),
                LocalDateTime.of(2026, 5, 20, 10, 0),
                1,
                "Y"
        );

        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(post("/api/admin/banners/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 배너 상태 변경이 차단된다")
    void updateActiveReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(patch("/api/admin/banners/active/3?isActive=N"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("배너 삭제 API는 성공 응답을 반환한다")
    void deleteReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/banners/delete").param("no", "3"))
                .andExpect(status().isOk());
    }
}
