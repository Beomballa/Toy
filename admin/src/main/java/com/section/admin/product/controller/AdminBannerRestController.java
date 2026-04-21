package com.section.admin.product.controller;

import com.section.admin.product.service.AdminBannerService;
import com.section.common.commerce.entity.DisplayBanner;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/banners")
public class AdminBannerRestController {

    private final AdminBannerService adminBannerService;

    @GetMapping("/list")
    public ResponseEntity<List<DisplayBanner>> getList() {
        return ResponseEntity.ok(adminBannerService.getBannerList());
    }

    @PostMapping("/save")
    public ResponseEntity<Void> save(@RequestBody DisplayBanner banner) {
        adminBannerService.saveBanner(banner);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long bannerNo) {
        adminBannerService.deleteBanner(bannerNo);
        return ResponseEntity.ok().build();
    }
}
