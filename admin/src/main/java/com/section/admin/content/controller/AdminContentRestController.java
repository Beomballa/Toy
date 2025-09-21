package com.section.admin.content.controller;

import com.section.admin.base.res.BaseListResDto;
import com.section.admin.content.req.ContentSetReqDto;
import com.section.admin.content.service.AdminContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/content")
public class AdminContentRestController {

    private final AdminContentService adminContentService;

    @PostMapping("/save")
    public ResponseEntity<BaseListResDto> saveContent(@RequestBody ContentSetReqDto reqDto) {
        adminContentService.setContent(reqDto);
        return null;
    }
}
