package com.section.front.controller;

import com.section.front.content.dto.FrontContentDetailResponse;
import com.section.front.content.dto.FrontContentPageResponse;
import com.section.front.content.req.FrontContentListRequest;
import com.section.front.content.dto.FrontContentHighlightsResponse;
import com.section.front.content.dto.FrontContentItemResponse;
import com.section.front.content.dto.FrontPopularContentResponse;
import com.section.front.content.service.FrontContentService;
import com.section.front.content.service.FrontContentViewService;
import com.section.front.content.dto.FrontContentViewResponse;
import com.section.front.system.controller.FrontGlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrontContentRestControllerTest {

    private FrontContentService service;
    private FrontContentViewService viewService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(FrontContentService.class);
        viewService = mock(FrontContentViewService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new FrontContentRestController(service, viewService))
                .setControllerAdvice(new FrontGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("프론트 콘텐츠 API는 공지와 스타일 하이라이트를 반환한다")
    void returnsContentHighlights() throws Exception {
        when(service.getHighlights(4)).thenReturn(new FrontContentHighlightsResponse(
                List.of(new FrontContentItemResponse(1, "NOTICE", "배송 공지", "일정 안내", 10, true, "2026-07-22")),
                List.of(),
                List.of(new FrontPopularContentResponse(
                        2, "STYLE", "여름 스타일", "스타일 안내", 18, 12, false, "2026-07-21"
                )),
                "2026-07-17",
                "2026-07-23"
        ));

        mockMvc.perform(get("/api/front/content/highlights").param("limit", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices[0].title").value("배송 공지"))
                .andExpect(jsonPath("$.notices[0].pinned").value(true))
                .andExpect(jsonPath("$.styles").isArray())
                .andExpect(jsonPath("$.popular[0].recentViewCount").value(18))
                .andExpect(jsonPath("$.popular[0].uniqueVisitors").value(12))
                .andExpect(jsonPath("$.popularStartDate").value("2026-07-17"))
                .andExpect(jsonPath("$.popularEndDate").value("2026-07-23"));
    }

    @Test
    @DisplayName("잘못된 콘텐츠 제한값은 표준 F001 오류를 반환한다")
    void invalidLimitReturnsBadRequestContract() throws Exception {
        when(service.getHighlights(9)).thenThrow(new IllegalArgumentException("invalid limit"));

        mockMvc.perform(get("/api/front/content/highlights").param("limit", "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("F001"));
    }

    @Test
    @DisplayName("프론트 콘텐츠 상세 API는 본문과 연관 콘텐츠를 반환한다")
    void returnsContentDetail() throws Exception {
        when(service.findDetail(1L)).thenReturn(Optional.of(new FrontContentDetailResponse(
                1L, "NOTICE", "배송 공지", "배송 일정 안내", 10, true, "2026-07-22", List.of()
        )));

        mockMvc.perform(get("/api/front/content/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("배송 공지"))
                .andExpect(jsonPath("$.content").value("배송 일정 안내"))
                .andExpect(jsonPath("$.relatedContents").isArray());
    }

    @Test
    @DisplayName("없는 공개 콘텐츠는 표준 404 오류를 반환한다")
    void missingContentReturnsNotFound() throws Exception {
        when(service.findDetail(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/front/content/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("F002"))
                .andExpect(jsonPath("$.message").value("콘텐츠를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("0 이하 콘텐츠 번호는 표준 400 오류를 반환한다")
    void nonPositiveContentIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/front/content/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("F001"));
    }

    @Test
    @DisplayName("프론트 콘텐츠 목록 API는 검색 결과와 페이징 메타를 반환한다")
    void returnsContentPage() throws Exception {
        when(service.search(any(FrontContentListRequest.class))).thenReturn(new FrontContentPageResponse(
                List.of(new FrontContentItemResponse(1, "NOTICE", "배송 공지", "일정 안내", 10, true, "2026-07-22")),
                0, 8, 1, 1, true, true
        ));

        mockMvc.perform(get("/api/front/content").param("boardType", "NOTICE").param("keyword", "배송"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("배송 공지"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    @DisplayName("지원하지 않는 콘텐츠 게시판 유형은 표준 400 오류를 반환한다")
    void rejectsUnsupportedContentBoard() throws Exception {
        when(service.search(any(FrontContentListRequest.class)))
                .thenThrow(new IllegalArgumentException("unsupported board"));

        mockMvc.perform(get("/api/front/content").param("boardType", "QNA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("F001"));
    }

    @Test
    @DisplayName("콘텐츠 조회 기록 API는 중복 여부와 현재 조회수를 반환한다")
    void recordsContentView() throws Exception {
        String visitorKey = "123e4567-e89b-12d3-a456-426614174000";
        when(viewService.record(1L, visitorKey)).thenReturn(new FrontContentViewResponse(true, 11));

        mockMvc.perform(post("/api/front/content/1/views")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorKey\":\"" + visitorKey + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counted").value(true))
                .andExpect(jsonPath("$.viewCount").value(11));
    }

    @Test
    @DisplayName("잘못된 방문자 키는 표준 400 오류를 반환한다")
    void invalidVisitorKeyReturnsBadRequest() throws Exception {
        when(viewService.record(1L, "short")).thenThrow(new IllegalArgumentException("invalid visitor"));

        mockMvc.perform(post("/api/front/content/1/views")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorKey\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("F001"));
    }

    @Test
    @DisplayName("조회 기록 요청 본문이 없으면 표준 400 오류를 반환한다")
    void missingViewRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/front/content/1/views")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("F001"));
    }
}
