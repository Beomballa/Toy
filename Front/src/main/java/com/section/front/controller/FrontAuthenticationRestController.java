package com.section.front.controller;

import com.section.front.auth.dto.FrontMemberLoginRequest;
import com.section.front.auth.dto.FrontMemberPasswordChangeRequest;
import com.section.front.auth.dto.FrontMemberProfileUpdateRequest;
import com.section.front.auth.dto.FrontMemberResponse;
import com.section.front.auth.dto.FrontMemberSignUpRequest;
import com.section.front.auth.service.FrontAuthenticationService;
import com.section.front.auth.support.FrontLoginAttemptGuard;
import com.section.front.auth.support.FrontMemberSession;
import com.section.front.auth.support.FrontMemberSession.AuthenticatedFrontMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front/auth")
public class FrontAuthenticationRestController {

    private final FrontAuthenticationService authenticationService;
    private final FrontLoginAttemptGuard loginAttemptGuard;

    @GetMapping("/me")
    public FrontMemberResponse me(HttpServletRequest request) {
        AuthenticatedFrontMember member = FrontMemberSession.read(request.getSession(false));
        return member == null ? FrontMemberResponse.anonymous() : FrontMemberResponse.authenticated(member);
    }

    @PostMapping("/signup")
    public FrontMemberResponse signUp(
            @Valid @RequestBody FrontMemberSignUpRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthenticatedFrontMember member = authenticationService.signUp(request);
        storeAuthenticatedMember(httpRequest, member);
        return FrontMemberResponse.authenticated(member);
    }

    @PostMapping("/login")
    public FrontMemberResponse login(
            @Valid @RequestBody FrontMemberLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String remoteAddress = httpRequest.getRemoteAddr();
        if (loginAttemptGuard.isBlocked(remoteAddress, request.email())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 많습니다. 잠시 후 다시 시도해 주세요.");
        }

        AuthenticatedFrontMember member = authenticationService.authenticate(request.email(), request.password())
                .orElseThrow(() -> {
                    loginAttemptGuard.recordFailure(remoteAddress, request.email());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해 주세요.");
                });
        loginAttemptGuard.clear(remoteAddress, request.email());
        storeAuthenticatedMember(httpRequest, member);
        return FrontMemberResponse.authenticated(member);
    }

    @PostMapping("/logout")
    public FrontMemberResponse logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return FrontMemberResponse.anonymous();
    }

    @PostMapping("/password")
    public FrontMemberResponse changePassword(
            @Valid @RequestBody FrontMemberPasswordChangeRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthenticatedFrontMember sessionMember = FrontMemberSession.read(httpRequest.getSession(false));
        if (sessionMember == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 후 비밀번호를 변경할 수 있습니다.");
        }

        AuthenticatedFrontMember member = authenticationService.changePassword(sessionMember.memberId(), request);
        // Rotate the session identifier after a sensitive credential update.
        storeAuthenticatedMember(httpRequest, member);
        return FrontMemberResponse.authenticated(member);
    }

    @PostMapping("/profile")
    public FrontMemberResponse updateProfile(
            @Valid @RequestBody FrontMemberProfileUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthenticatedFrontMember sessionMember = FrontMemberSession.read(httpRequest.getSession(false));
        if (sessionMember == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 후 기본정보를 수정할 수 있습니다.");
        }
        AuthenticatedFrontMember member = authenticationService.updateProfile(sessionMember.memberId(), request);
        FrontMemberSession.store(httpRequest.getSession(), member);
        return FrontMemberResponse.authenticated(member);
    }

    private void storeAuthenticatedMember(HttpServletRequest request, AuthenticatedFrontMember member) {
        HttpSession session = request.getSession(true);
        if (!session.isNew()) {
            request.changeSessionId();
        }
        FrontMemberSession.store(session, member);
    }
}
