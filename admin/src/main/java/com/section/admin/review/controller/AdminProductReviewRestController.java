package com.section.admin.review.controller;

import com.section.admin.review.req.AdminProductReviewStatusRequest;
import com.section.admin.review.res.AdminProductReviewListResponse;
import com.section.admin.review.service.AdminProductReviewService;
import com.section.admin.settings.service.AdminOperationPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
public class AdminProductReviewRestController {

    private final AdminProductReviewService reviewService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping
    public AdminProductReviewListResponse getReviews(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "false") boolean reportedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reviewService.getReviews(status, reportedOnly, page, size);
    }

    @PatchMapping("/{reviewId}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable long reviewId,
            @Valid @RequestBody AdminProductReviewStatusRequest request
    ) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        reviewService.changeStatus(reviewId, request.status());
        return ResponseEntity.noContent().build();
    }
}
