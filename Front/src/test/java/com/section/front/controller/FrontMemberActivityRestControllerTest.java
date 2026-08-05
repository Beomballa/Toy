package com.section.front.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.common.commerce.entity.FrontMemberActivityType;
import com.section.front.memberactivity.dto.FrontMemberActivityResponse;
import com.section.front.memberactivity.service.FrontMemberActivityService;
import com.section.front.system.controller.FrontGlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrontMemberActivityRestControllerTest {

    private final FrontMemberActivityService activityService = mock(FrontMemberActivityService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FrontMemberActivityRestController(activityService))
                .setControllerAdvice(new FrontGlobalExceptionHandler())
                .build();
    }

    @Test
    void requiresAuthenticatedMemberSession() throws Exception {
        mockMvc.perform(get("/api/front/member-activities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("F006"));
    }

    @Test
    void replacesAndClearsAuthenticatedMemberActivity() throws Exception {
        MockHttpSession session = authenticatedSession();
        FrontMemberActivityResponse response = new FrontMemberActivityResponse(
                Map.of("BOOKMARK", List.of()),
                Map.of("BOOKMARK", 24)
        );
        given(activityService.replace(7L, FrontMemberActivityType.BOOKMARK, List.of(3L, 2L))).willReturn(response);
        given(activityService.clear(7L, FrontMemberActivityType.BOOKMARK)).willReturn(response);

        mockMvc.perform(put("/api/front/member-activities/BOOKMARK")
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("productIds", List.of(3L, 2L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limits.BOOKMARK").value(24));
        mockMvc.perform(delete("/api/front/member-activities/BOOKMARK").session(session))
                .andExpect(status().isOk());

        verify(activityService).replace(eq(7L), eq(FrontMemberActivityType.BOOKMARK), eq(List.of(3L, 2L)));
        verify(activityService).clear(7L, FrontMemberActivityType.BOOKMARK);
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("frontMemberNo", 7L);
        session.setAttribute("frontMemberEmail", "member@example.com");
        session.setAttribute("frontMemberName", "회원");
        session.setAttribute("frontMemberNickname", "노렌");
        return session;
    }
}
