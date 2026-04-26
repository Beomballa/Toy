package com.section.admin.brand.controller;

import com.section.admin.brand.req.BrandSaveRequest;
import com.section.admin.brand.res.BrandResponse;
import com.section.admin.brand.service.AdminBrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/brands")
public class AdminBrandRestController {

    private final AdminBrandService adminBrandService;

    @GetMapping("/list")
    public ResponseEntity<List<BrandResponse>> getList() {
        return ResponseEntity.ok(adminBrandService.getBrandList());
    }

    @GetMapping("/get")
    public ResponseEntity<BrandResponse> getDetail(@RequestParam("no") Long brandNo) {
        return ResponseEntity.ok(adminBrandService.getBrand(brandNo));
    }

    @PostMapping("/save")
    public ResponseEntity<Void> save(@Valid @RequestBody BrandSaveRequest req) {
        adminBrandService.saveBrand(req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long brandNo) {
        adminBrandService.deleteBrand(brandNo);
        return ResponseEntity.ok().build();
    }
}
