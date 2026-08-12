package com.section.front.controller;

import com.section.front.auth.support.FrontMemberSession;
import com.section.front.auth.support.FrontMemberSession.AuthenticatedFrontMember;
import com.section.front.productreview.dto.FrontProductReviewCreateRequest;
import com.section.front.productreview.dto.FrontProductReviewReportRequest;
import com.section.front.productreview.dto.FrontReviewEligibleOrderResponse;
import com.section.front.productreview.dto.FrontProductReviewPageResponse;
import com.section.front.productreview.dto.FrontProductReviewResponse;
import com.section.front.productreview.service.FrontProductReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front/products/{productId}/reviews")
public class FrontProductReviewRestController {

    private final FrontProductReviewService reviewService;

    @GetMapping
    public FrontProductReviewPageResponse getReviews(
            @PathVariable long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "RECENT") String sort,
            HttpServletRequest httpRequest
    ) {
        AuthenticatedFrontMember member = FrontMemberSession.read(httpRequest.getSession(false));
        return reviewService.getReviews(productId, page, sort, member == null ? null : member.memberId());
    }

    @PostMapping
    public FrontProductReviewResponse createReview(
            @PathVariable long productId,
            @Valid @RequestBody FrontProductReviewCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        return reviewService.createReview(memberNo(httpRequest), productId, request);
    }

    @PostMapping("/{reviewId}/reports")
    public void reportReview(
            @PathVariable long reviewId,
            @Valid @RequestBody FrontProductReviewReportRequest request,
            HttpServletRequest httpRequest
    ) {
        reviewService.reportReview(memberNo(httpRequest), reviewId, request);
    }

    @GetMapping("/eligible-orders")
    public List<FrontReviewEligibleOrderResponse> getEligibleOrders(
            @PathVariable long productId,
            HttpServletRequest httpRequest
    ) {
        return reviewService.getEligibleOrders(memberNo(httpRequest), productId);
    }

    private long memberNo(HttpServletRequest request) {
        AuthenticatedFrontMember member = FrontMemberSession.read(request.getSession(false));
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return member.memberId();
    }
}
