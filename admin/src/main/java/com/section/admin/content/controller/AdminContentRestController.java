package com.section.admin.content.controller;

import com.section.admin.base.res.BaseListResDto;
import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.content.req.ContentListReqDto;
import com.section.admin.content.req.ContentSetReqDto;
import com.section.admin.content.req.UpdateViewCountReqDto;
import com.section.admin.content.res.ContentMyDocResDto;
import com.section.admin.content.res.CreateDocumentDefaultInfoResDto;
import com.section.admin.content.service.AdminContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/content")
public class AdminContentRestController {

    private final AdminContentService adminContentService;

    @PostMapping("/list")
    public ResponseEntity<BaseSimpleResDto> listDocument(@RequestBody ContentListReqDto reqDto) {
        ContentMyDocResDto documentList = adminContentService.listDocument(reqDto);
        return new ResponseEntity<>(new BaseListResDto(documentList.getDocuments(), documentList.getTotalCount()), HttpStatus.OK);
    }

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

    @PostMapping("/update/cnt")
    public ResponseEntity<BaseSimpleResDto> updateViewCount(@RequestBody UpdateViewCountReqDto reqDto) {
        adminContentService.updateViewCount(reqDto);
        return new ResponseEntity<>(new BaseSimpleResDto(), HttpStatus.OK);
    }
}
