package com.section.admin.product.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.product.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminProductRestController {

    private final AdminProductService adminProductService;

    @PostMapping("/set/info")
    public ResponseEntity<BaseSimpleResDto> defaultProductSetInfo() {
        log.info("test");

//        ContentMyDocResDto documentList = adminContentService.listDocument(reqDto);
//        BaseListResDto resDto = new BaseListResDto(documentList.getDocuments(), documentList.getTotalCount());
//        if (documentList.getTotalCount() > 0) {
//            resDto.setResultMsg("Y");
//        } else {
//            resDto.setResultMsg("N");
//        }
//        return new ResponseEntity<>(resDto, HttpStatus.OK);
        return null;
    }

}
