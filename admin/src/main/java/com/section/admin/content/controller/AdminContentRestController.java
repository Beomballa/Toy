package com.section.admin.content.controller;

import com.section.admin.base.res.BaseListResDto;
import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.content.req.ContentSetReqDto;
import com.section.admin.content.res.CreateDocumentDefaultInfoResDto;
import com.section.admin.content.service.AdminContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/set")
    public ResponseEntity<BaseSimpleResDto> setDocument() {
        CreateDocumentDefaultInfoResDto result = adminContentService.setDocument();
        if(result != null) {
            return new ResponseEntity<>(new BaseSimpleResDto(), HttpStatus.OK);
        }else {
            return new ResponseEntity<>(new BaseSimpleResDto(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/save")
    public ResponseEntity<BaseSimpleResDto> saveContent(@RequestBody ContentSetReqDto reqDto) {
        adminContentService.setContent(reqDto);
        return new ResponseEntity<>(new BaseSimpleResDto(), HttpStatus.OK);
    }
}
