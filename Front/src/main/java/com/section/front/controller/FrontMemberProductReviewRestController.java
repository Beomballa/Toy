package com.section.front.controller;

import com.section.front.auth.support.FrontMemberSession;
import com.section.front.auth.support.FrontMemberSession.AuthenticatedFrontMember;
import com.section.front.productreview.dto.FrontMemberProductReviewPageResponse;
import com.section.front.productreview.service.FrontProductReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front/member/reviews")
public class FrontMemberProductReviewRestController {

    private final FrontProductReviewService reviewService;

    @GetMapping
    public FrontMemberProductReviewPageResponse getReviews(
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request
    ) {
        AuthenticatedFrontMember member = FrontMemberSession.read(request.getSession(false));
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return reviewService.getMemberReviews(member.memberId(), page);
    }
}
