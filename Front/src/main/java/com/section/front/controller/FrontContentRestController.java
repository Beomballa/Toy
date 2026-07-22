package com.section.front.controller;

import com.section.front.content.dto.FrontContentHighlightsResponse;
import com.section.front.content.service.FrontContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front/content")
public class FrontContentRestController {

    private final FrontContentService frontContentService;

    @GetMapping("/highlights")
    public FrontContentHighlightsResponse getHighlights(
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return frontContentService.getHighlights(limit);
    }
}
