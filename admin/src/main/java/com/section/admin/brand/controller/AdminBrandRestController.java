package com.section.admin.brand.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.brand.req.BrandListRequest;
import com.section.admin.brand.req.BrandSaveRequest;
import com.section.admin.brand.req.BrandStatusUpdateRequest;
import com.section.admin.brand.res.BrandListResponse;
import com.section.admin.brand.res.BrandResponse;
import com.section.admin.brand.service.AdminBrandService;
import com.section.admin.settings.service.AdminOperationPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/brands")
public class AdminBrandRestController {

    private final AdminBrandService adminBrandService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<BrandListResponse> getList(@ModelAttribute BrandListRequest req) {
        return ResponseEntity.ok(adminBrandService.getBrandList(req));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute BrandListRequest req) {
        String fileName = "brands-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(adminBrandService.exportBrandListCsv(req));
    }

    @GetMapping("/get")
    public ResponseEntity<BrandResponse> getDetail(@RequestParam("no") Long brandNo) {
        return ResponseEntity.ok(adminBrandService.getBrand(brandNo));
    }

    @PostMapping("/save")
    public ResponseEntity<Void> save(@Valid @RequestBody BrandSaveRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminBrandService.saveBrand(req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/active/{no}")
    public ResponseEntity<BaseSimpleResDto> updateActive(@PathVariable("no") Long brandNo, @Valid @RequestBody BrandStatusUpdateRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminBrandService.updateActive(brandNo, req.isActive());
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long brandNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminBrandService.deleteBrand(brandNo);
        return ResponseEntity.ok().build();
    }
}
