package com.section.admin.product.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.product.req.ProductCreateRequest;
import com.section.common.commerce.dto.ProductCreateReqDto;
import com.section.admin.product.service.AdminProductService;
import jakarta.validation.Valid;
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

    @PostMapping("/product/set")
    public ResponseEntity<BaseSimpleResDto> defaultProductSetInfo(@Valid @RequestBody ProductCreateRequest reqDto) {
        log.info("상품 등록 요청 : {}", reqDto);

        adminProductService.createProductInfo(reqDto);

        return ResponseEntity.ok(new BaseSimpleResDto());
    }
}
