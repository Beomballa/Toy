package com.section.admin.product.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductListRequest;
import com.section.admin.product.res.ProductListResponse;
import com.section.common.commerce.dto.ProductCreateReqDto;
import com.section.admin.product.service.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminProductRestController {

    private final AdminProductService adminProductService;

    @GetMapping("/product/list")
    public ResponseEntity<Page<ProductListResponse.ProductListItem>> getProductList(
            @ModelAttribute ProductListRequest req, Pageable pageable
    ) {

        log.info("상품 목록 조회 요청 : {}, 페이징 : {}", req, pageable);
        Page<ProductListResponse.ProductListItem> result = adminProductService.getProductList(req, pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/product/set")
    public ResponseEntity<BaseSimpleResDto> defaultProductSetInfo(@Valid @RequestBody ProductCreateRequest reqDto) {
        log.info("상품 등록 요청 : {}", reqDto);

        adminProductService.createProductInfo(reqDto);

        return ResponseEntity.ok(new BaseSimpleResDto());
    }
}
