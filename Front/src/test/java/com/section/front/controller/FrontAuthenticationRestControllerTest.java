package com.section.front.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.front.auth.service.FrontAuthenticationService;
import com.section.front.auth.support.FrontLoginAttemptGuard;
import com.section.front.auth.support.FrontMemberSession.AuthenticatedFrontMember;
import com.section.front.system.controller.FrontGlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrontAuthenticationRestControllerTest {

    private final FrontAuthenticationService authenticationService = mock(FrontAuthenticationService.class);
    private final FrontLoginAttemptGuard loginAttemptGuard = mock(FrontLoginAttemptGuard.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new FrontAuthenticationRestController(authenticationService, loginAttemptGuard)
                )
                .setControllerAdvice(new FrontGlobalExceptionHandler())
                .build();
    }

    @Test
    void logsInAndStoresMemberSession() throws Exception {
        AuthenticatedFrontMember member = new AuthenticatedFrontMember(7L, "member@example.com", "회원", "노렌");
        given(authenticationService.authenticate("member@example.com", "noren1234"))
                .willReturn(Optional.of(member));

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/front/auth/login")
                        .session(session)
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.10");
                            return request;
                        })
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "member@example.com",
                                "password", "noren1234"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.nickname").value("노렌"));

        verify(loginAttemptGuard).clear("192.0.2.10", "member@example.com");
    }

    @Test
    void recordsFailedLoginWithoutCreatingAuthenticatedSession() throws Exception {
        given(authenticationService.authenticate("unknown@example.com", "wrong1234"))
                .willReturn(Optional.empty());

        mockMvc.perform(post("/api/front/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.11");
                            return request;
                        })
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "unknown@example.com",
                                "password", "wrong1234"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("F006"));

        verify(loginAttemptGuard).recordFailure("192.0.2.11", "unknown@example.com");
    }

    @Test
    void returnsCurrentSessionAndInvalidatesItOnLogout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("frontMemberNo", 9L);
        session.setAttribute("frontMemberEmail", "member@example.com");
        session.setAttribute("frontMemberName", "회원");
        session.setAttribute("frontMemberNickname", "노렌");

        mockMvc.perform(get("/api/front/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(9L));

        mockMvc.perform(post("/api/front/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void changesPasswordForAuthenticatedMember() throws Exception {
        AuthenticatedFrontMember member = new AuthenticatedFrontMember(9L, "member@example.com", "회원", "노렌");
        given(authenticationService.changePassword(any(Long.class), any())).willReturn(member);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("frontMemberNo", 9L);
        session.setAttribute("frontMemberEmail", "member@example.com");
        session.setAttribute("frontMemberName", "회원");
        session.setAttribute("frontMemberNickname", "노렌");

        mockMvc.perform(post("/api/front/auth/password")
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "noren1234",
                                "newPassword", "renew1234"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));

        verify(authenticationService).changePassword(any(Long.class), any());
    }

    @Test
    void rejectsPasswordChangeWithoutAuthenticatedSession() throws Exception {
        mockMvc.perform(post("/api/front/auth/password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "noren1234",
                                "newPassword", "renew1234"
                        ))))
                .andExpect(status().isUnauthorized());
    }
}
