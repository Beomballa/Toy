package com.section.front.controller;

import com.section.front.content.dto.FrontContentDetailResponse;
import com.section.front.content.dto.FrontContentHighlightsResponse;
import com.section.front.content.dto.FrontContentPageResponse;
import com.section.front.content.dto.FrontContentReactionResponse;
import com.section.front.content.dto.FrontContentViewResponse;
import com.section.front.content.exception.FrontContentNotFoundException;
import com.section.front.content.req.FrontContentListRequest;
import com.section.front.content.req.FrontContentReactionRequest;
import com.section.front.content.req.FrontContentViewRequest;
import com.section.front.content.service.FrontContentReactionService;
import com.section.front.content.service.FrontContentService;
import com.section.front.content.service.FrontContentViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front/content")
public class FrontContentRestController {

    private final FrontContentService frontContentService;
    private final FrontContentViewService frontContentViewService;
    private final FrontContentReactionService frontContentReactionService;

    @GetMapping
    public FrontContentPageResponse getContents(@ModelAttribute FrontContentListRequest request) {
        return frontContentService.search(request);
    }

    @GetMapping("/highlights")
    public FrontContentHighlightsResponse getHighlights(
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return frontContentService.getHighlights(limit);
    }

    @GetMapping("/{documentId}")
    public FrontContentDetailResponse getDetail(@PathVariable long documentId) {
        if (documentId <= 0) {
            throw new IllegalArgumentException("콘텐츠 ID는 양수여야 합니다.");
        }
        return frontContentService.findDetail(documentId)
                .orElseThrow(FrontContentNotFoundException::new);
    }

    @PostMapping("/{documentId}/views")
    public FrontContentViewResponse recordView(
            @PathVariable long documentId,
            @RequestBody FrontContentViewRequest request
    ) {
        if (documentId <= 0) {
            throw new IllegalArgumentException("콘텐츠 ID는 양수여야 합니다.");
        }
        return frontContentViewService.record(documentId, request.visitorKey());
    }

    @GetMapping("/{documentId}/reactions")
    public FrontContentReactionResponse getReactions(
            @PathVariable long documentId,
            @RequestHeader("X-Content-Visitor-Key") String visitorKey
    ) {
        validateDocumentId(documentId);
        return frontContentReactionService.getSummary(documentId, visitorKey);
    }

    @PostMapping("/{documentId}/reactions")
    public FrontContentReactionResponse react(
            @PathVariable long documentId,
            @RequestBody FrontContentReactionRequest request
    ) {
        validateDocumentId(documentId);
        return frontContentReactionService.react(documentId, request.visitorKey(), request.reaction());
    }

    private void validateDocumentId(long documentId) {
        if (documentId <= 0) {
            throw new IllegalArgumentException("콘텐츠 ID는 양수여야 합니다.");
        }
    }
}
