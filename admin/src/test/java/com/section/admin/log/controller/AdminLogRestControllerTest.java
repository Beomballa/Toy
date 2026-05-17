package com.section.admin.log.controller;

import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.log.res.AdminLogDetailResponse;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminLogRestControllerTest {

    @Mock
    private AdminLogService adminLogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminLogRestController(adminLogService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("활동 로그 목록 API는 페이지 응답을 반환한다")
    void getLogListReturnsPagedResponse() throws Exception {
        when(adminLogService.getLogList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminLogListResponse(
                        List.of(new AdminLogListResponse.Item(1L, 2L, "운영자", "PRODUCT_UPDATE", 4L, "상품 #4", "/admin/products/history?productNo=4", "127.0.0.1", "2026-05-11 12:00")),
                        1L, 1, 0, 20, 1L, 1L, "1-1 / 1건 · 1페이지",
                        new AdminLogListResponse.AppliedQuery(null, null, null, null, null),
                        new AdminLogListResponse.ResultMeta("검색 결과 1건", "1-1 / 1건 · 1페이지", 0, "1-1")
                ));

        mockMvc.perform(get("/api/admin/logs/list?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].adminName").value("운영자"))
                .andExpect(jsonPath("$.items[0].targetLabel").value("상품 #4"))
                .andExpect(jsonPath("$.items[0].targetPath").value("/admin/products/history?productNo=4"))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.pageInfoLabel").value("1-1 / 1건 · 1페이지"))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("검색 결과 1건"))
                .andExpect(jsonPath("$.totalElements").value(1L));
    }

    @Test
    @DisplayName("활동 로그 상세 API는 상세 응답을 반환한다")
    void getLogDetailReturnsDetailResponse() throws Exception {
        when(adminLogService.getLogDetail(9L))
                .thenReturn(new AdminLogDetailResponse(9L, 3L, "배너담당", "BANNER_DELETE", 7L, "배너 #7", "/admin/banner/list", "127.0.0.1", "2026-05-11 13:00"));

        mockMvc.perform(get("/api/admin/logs/get?no=9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminName").value("배너담당"))
                .andExpect(jsonPath("$.actionType").value("BANNER_DELETE"))
                .andExpect(jsonPath("$.targetLabel").value("배너 #7"))
                .andExpect(jsonPath("$.targetPath").value("/admin/banner/list"));
    }

    @Test
    @DisplayName("활동 로그 목록 조회 중 잘못된 입력은 400으로 변환된다")
    void getLogListReturnsBadRequestWhenInputInvalid() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE))
                .when(adminLogService).getLogList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(get("/api/admin/logs/list?adminNo=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }
}
