package com.section.admin.content.controller;

import com.section.admin.base.res.BaseListResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/content")
public class AdminContentRestController {

    public ResponseEntity<BaseListResDto> saveContent() {

        return null;
    }
}
