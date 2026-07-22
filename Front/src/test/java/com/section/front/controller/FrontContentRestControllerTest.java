package com.section.front.controller;

import com.section.front.content.dto.FrontContentHighlightsResponse;
import com.section.front.content.dto.FrontContentItemResponse;
import com.section.front.content.service.FrontContentService;
import com.section.front.system.controller.FrontGlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrontContentRestControllerTest {

    private FrontContentService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(FrontContentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new FrontContentRestController(service))
                .setControllerAdvice(new FrontGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("프론트 콘텐츠 API는 공지와 스타일 하이라이트를 반환한다")
    void returnsContentHighlights() throws Exception {
        when(service.getHighlights(4)).thenReturn(new FrontContentHighlightsResponse(
                List.of(new FrontContentItemResponse(1, "NOTICE", "배송 공지", "일정 안내", 10, true, "2026-07-22")),
                List.of()
        ));

        mockMvc.perform(get("/api/front/content/highlights").param("limit", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices[0].title").value("배송 공지"))
                .andExpect(jsonPath("$.notices[0].pinned").value(true))
                .andExpect(jsonPath("$.styles").isArray());
    }

    @Test
    @DisplayName("잘못된 콘텐츠 제한값은 표준 F001 오류를 반환한다")
    void invalidLimitReturnsBadRequestContract() throws Exception {
        when(service.getHighlights(9)).thenThrow(new IllegalArgumentException("invalid limit"));

        mockMvc.perform(get("/api/front/content/highlights").param("limit", "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("F001"));
    }
}
