package com.section.front.controller;

import com.section.common.commerce.entity.FrontMemberActivityType;
import com.section.front.auth.support.FrontMemberSession;
import com.section.front.auth.support.FrontMemberSession.AuthenticatedFrontMember;
import com.section.front.memberactivity.dto.FrontMemberActivityReplaceRequest;
import com.section.front.memberactivity.dto.FrontMemberActivityResponse;
import com.section.front.memberactivity.dto.FrontMemberActivitySyncRequest;
import com.section.front.memberactivity.service.FrontMemberActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front/member-activities")
public class FrontMemberActivityRestController {

    private final FrontMemberActivityService activityService;

    @GetMapping
    public FrontMemberActivityResponse getActivities(HttpServletRequest request) {
        return activityService.getActivities(memberNo(request));
    }

    @PutMapping("/sync")
    public FrontMemberActivityResponse sync(
            @Valid @RequestBody FrontMemberActivitySyncRequest syncRequest,
            HttpServletRequest request
    ) {
        return activityService.sync(memberNo(request), syncRequest.activities());
    }

    @PutMapping("/{type}")
    public FrontMemberActivityResponse replace(
            @PathVariable FrontMemberActivityType type,
            @Valid @RequestBody FrontMemberActivityReplaceRequest replaceRequest,
            HttpServletRequest request
    ) {
        return activityService.replace(memberNo(request), type, replaceRequest.productIds());
    }

    @PostMapping("/{type}/products/{productNo}")
    public FrontMemberActivityResponse add(
            @PathVariable FrontMemberActivityType type,
            @PathVariable long productNo,
            HttpServletRequest request
    ) {
        return activityService.add(memberNo(request), type, productNo);
    }

    @DeleteMapping("/{type}/products/{productNo}")
    public FrontMemberActivityResponse remove(
            @PathVariable FrontMemberActivityType type,
            @PathVariable long productNo,
            HttpServletRequest request
    ) {
        return activityService.remove(memberNo(request), type, productNo);
    }

    @DeleteMapping("/{type}")
    public FrontMemberActivityResponse clear(
            @PathVariable FrontMemberActivityType type,
            HttpServletRequest request
    ) {
        return activityService.clear(memberNo(request), type);
    }

    @DeleteMapping
    public FrontMemberActivityResponse clearAll(HttpServletRequest request) {
        return activityService.clearAll(memberNo(request));
    }

    private long memberNo(HttpServletRequest request) {
        AuthenticatedFrontMember member = FrontMemberSession.read(request.getSession(false));
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return member.memberId();
    }
}
