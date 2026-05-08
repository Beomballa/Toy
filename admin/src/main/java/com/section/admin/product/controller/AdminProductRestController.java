package com.section.admin.product.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductListRequest;
import com.section.admin.product.req.ProductUpdateRequest;
import com.section.admin.product.res.ProductCreateResponse;
import com.section.admin.product.res.ProductDetailResponse;
import com.section.admin.product.res.ProductListResponse;
import com.section.admin.product.service.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminProductRestController {

    private final AdminProductService adminProductService;

    @GetMapping("/product/list")
    public ResponseEntity<ProductListResponse> getProductList(
            @ModelAttribute ProductListRequest req, Pageable pageable
    ) {
        log.info("상품 목록 조회 요청 : {}, 페이징 : {}", req, pageable);
        ProductListResponse result = adminProductService.getProductList(req, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/product/export")
    public ResponseEntity<byte[]> exportProductList(@ModelAttribute ProductListRequest req) {
        String exportFilename = buildExportFilename(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + exportFilename)
                .contentType(new MediaType("text", "csv"))
                .body(adminProductService.exportProductListCsv(req));
    }

    @PostMapping("/product/set")
    public ResponseEntity<ProductCreateResponse> defaultProductSetInfo(@Valid @RequestBody ProductCreateRequest reqDto) {
        log.info("상품 등록 요청 : {}", reqDto);

        Long productNo = adminProductService.createProductInfo(reqDto);

        return ResponseEntity.ok(ProductCreateResponse.success(productNo));
    }

    @PostMapping("/product/update")
    public ResponseEntity<BaseSimpleResDto> updateProductInfo(@Valid @RequestBody ProductUpdateRequest reqDto) {
        log.info("상품 수정 요청 : {}", reqDto);

        adminProductService.updateProductInfo(reqDto);

        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @GetMapping("/product/get")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@RequestParam("no") Long productNo) {
        return ResponseEntity.ok(adminProductService.getProductDetail(productNo));
    }

    @PatchMapping("/product/delete/{no}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("no") Long productNo) {
        adminProductService.deleteProduct(productNo);
        return ResponseEntity.ok().build();
    }

    private String buildExportFilename(ProductListRequest req) {
        List<String> parts = new ArrayList<>();
        parts.add("products");
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            parts.add(req.getStatus().trim().toLowerCase(Locale.ROOT));
        }
        if (Boolean.TRUE.equals(req.getLowStockOnly())) {
            parts.add("lowstock");
        }
        if (Boolean.TRUE.equals(req.getCreatedTodayOnly())) {
            parts.add("today");
        }
        if (req.getBrandNo() != null && req.getBrandNo() > 0) {
            parts.add("brand" + req.getBrandNo());
        }
        if (req.getCategoryNo() != null && req.getCategoryNo() > 0) {
            parts.add("category" + req.getCategoryNo());
        }
        if (req.getSearchKeyword() != null && !req.getSearchKeyword().isBlank()) {
            parts.add("search");
        }
        parts.add(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        return String.join("_", parts) + ".csv";
    }
}
