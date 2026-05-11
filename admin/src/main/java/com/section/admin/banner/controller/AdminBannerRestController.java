package com.section.admin.banner.controller;

import com.section.admin.banner.req.BannerListRequest;
import com.section.admin.banner.req.BannerSaveRequest;
import com.section.admin.banner.res.BannerListResponse;
import com.section.admin.banner.service.AdminBannerService;
import com.section.admin.base.res.BaseSimpleResDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/banners")
public class AdminBannerRestController {

    private final AdminBannerService adminBannerService;

    @GetMapping("/list")
    public ResponseEntity<BannerListResponse> getList(@ModelAttribute BannerListRequest req) {
        return ResponseEntity.ok(adminBannerService.getBannerList(req));
    }

    @PostMapping("/save")
    public ResponseEntity<BaseSimpleResDto> save(@Valid @RequestBody BannerSaveRequest req) {
        adminBannerService.saveBanner(req);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @PatchMapping("/active/{no}")
    public ResponseEntity<BaseSimpleResDto> updateActive(@PathVariable("no") Long bannerNo, @RequestParam("isActive") String isActive) {
        adminBannerService.updateActive(bannerNo, isActive);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long bannerNo) {
        adminBannerService.deleteBanner(bannerNo);
        return ResponseEntity.ok().build();
    }
}
