package com.section.admin.product.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.product.req.ProductBulkDeleteRequest;
import com.section.admin.product.req.ProductBulkDuplicateRequest;
import com.section.admin.product.req.ProductBulkOperateRequest;
import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductFrontDisplayListRequest;
import com.section.admin.product.req.ProductFrontDisplaySaveRequest;
import com.section.admin.product.req.ProductHistoryListRequest;
import com.section.admin.product.req.ProductListRequest;
import com.section.admin.product.req.ProductUpdateRequest;
import com.section.admin.product.res.ProductCreateResponse;
import com.section.admin.product.res.ProductDetailResponse;
import com.section.admin.product.res.ProductFrontDisplayDashboardResponse;
import com.section.admin.product.res.ProductFrontDisplayRankGuideResponse;
import com.section.admin.product.res.ProductFrontDisplayResponse;
import com.section.admin.product.res.ProductHistoryListResponse;
import com.section.admin.product.res.ProductHistoryResponse;
import com.section.admin.product.res.ProductListResponse;
import com.section.admin.product.service.AdminProductService;
import com.section.admin.settings.service.AdminOperationPolicyService;
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
    private final AdminOperationPolicyService adminOperationPolicyService;

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
        adminOperationPolicyService.assertAdminWriteAllowed();

        Long productNo = adminProductService.createProductInfo(reqDto);

        return ResponseEntity.ok(ProductCreateResponse.success(productNo));
    }

    @PostMapping("/product/update")
    public ResponseEntity<BaseSimpleResDto> updateProductInfo(@Valid @RequestBody ProductUpdateRequest reqDto) {
        log.info("상품 수정 요청 : {}", reqDto);
        adminOperationPolicyService.assertAdminWriteAllowed();

        adminProductService.updateProductInfo(reqDto);

        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @GetMapping("/product/get")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@RequestParam("no") Long productNo) {
        return ResponseEntity.ok(adminProductService.getProductDetail(productNo));
    }

    @GetMapping("/product/front-display")
    public ResponseEntity<ProductFrontDisplayResponse> getProductFrontDisplay(@RequestParam("productNo") Long productNo) {
        return ResponseEntity.ok(adminProductService.getFrontDisplay(productNo));
    }

    @GetMapping("/product/front-display/rank-guide")
    public ResponseEntity<ProductFrontDisplayRankGuideResponse> getProductFrontDisplayRankGuide(
            @RequestParam(value = "productNo", required = false) Long productNo
    ) {
        return ResponseEntity.ok(adminProductService.getFrontDisplayRankGuide(productNo));
    }

    @GetMapping("/product/front-display/list")
    public ResponseEntity<ProductFrontDisplayDashboardResponse> getProductFrontDisplayList(
            @ModelAttribute ProductFrontDisplayListRequest request
    ) {
        return ResponseEntity.ok(adminProductService.getFrontDisplayProducts(request));
    }

    @GetMapping("/product/front-display/export")
    public ResponseEntity<byte[]> exportProductFrontDisplayList(
            @ModelAttribute ProductFrontDisplayListRequest request
    ) {
        String exportFilename = buildFrontDisplayExportFilename(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + exportFilename)
                .contentType(new MediaType("text", "csv"))
                .body(adminProductService.exportFrontDisplayProductsCsv(request));
    }

    @PostMapping("/product/front-display")
    public ResponseEntity<ProductFrontDisplayResponse> saveProductFrontDisplay(
            @Valid @RequestBody ProductFrontDisplaySaveRequest request
    ) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminProductService.saveFrontDisplay(request));
    }

    @DeleteMapping("/product/front-display/{productNo}")
    public ResponseEntity<Void> clearProductFrontDisplay(@PathVariable Long productNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminProductService.clearFrontDisplay(productNo);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/product/history")
    public ResponseEntity<List<ProductHistoryResponse>> getProductHistory(@RequestParam("no") Long productNo) {
        return ResponseEntity.ok(adminProductService.getProductHistory(productNo));
    }

    @GetMapping("/product/history/list")
    public ResponseEntity<ProductHistoryListResponse> getProductHistoryList(
            @ModelAttribute ProductHistoryListRequest req,
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminProductService.getProductHistoryList(req, pageable));
    }

    @PatchMapping("/product/delete/{no}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("no") Long productNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminProductService.deleteProduct(productNo);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/product/bulk-operate")
    public ResponseEntity<AdminProductService.BulkOperateResult> bulkOperateProduct(@RequestBody ProductBulkOperateRequest reqDto) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminProductService.bulkOperateProducts(reqDto));
    }

    @PostMapping("/product/bulk-delete")
    public ResponseEntity<AdminProductService.BulkDeleteResult> bulkDeleteProduct(@RequestBody ProductBulkDeleteRequest reqDto) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminProductService.bulkDeleteProducts(reqDto));
    }

    @PostMapping("/product/bulk-duplicate")
    public ResponseEntity<AdminProductService.BulkDuplicateResult> bulkDuplicateProduct(@RequestBody ProductBulkDuplicateRequest reqDto) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminProductService.bulkDuplicateProducts(reqDto));
    }

    @PostMapping("/product/clone/{no}")
    public ResponseEntity<ProductCreateResponse> cloneProduct(@PathVariable("no") Long productNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(ProductCreateResponse.success(adminProductService.cloneProduct(productNo)));
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

    private String buildFrontDisplayExportFilename(ProductFrontDisplayListRequest req) {
        List<String> parts = new ArrayList<>();
        parts.add("front_display");
        if (req.normalizedStatus() != null) {
            parts.add(req.normalizedStatus().name().toLowerCase(Locale.ROOT));
        }
        if (req.normalizedBrandNo() != null) {
            parts.add("brand" + req.normalizedBrandNo());
        }
        if (req.normalizedCategoryNo() != null) {
            parts.add("category" + req.normalizedCategoryNo());
        }
        if (Boolean.TRUE.equals(req.normalizedConfigured())) {
            parts.add("configured");
        } else if (Boolean.FALSE.equals(req.normalizedConfigured())) {
            parts.add("unconfigured");
        }
        if (req.normalizedFeaturedOnly()) {
            parts.add("featured");
        }
        if (req.normalizedLowStockOnly()) {
            parts.add("lowstock");
        }
        if (req.normalizedKeyword() != null) {
            parts.add("search");
        }
        parts.add(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        return String.join("_", parts) + ".csv";
    }
}
