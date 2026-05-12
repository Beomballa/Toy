package com.section.admin.banner.controller;

import com.section.admin.banner.req.BannerListRequest;
import com.section.admin.banner.req.BannerSaveRequest;
import com.section.admin.banner.res.BannerListResponse;
import com.section.admin.banner.service.AdminBannerService;
import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.settings.service.AdminOperationPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/banners")
public class AdminBannerRestController {

    private final AdminBannerService adminBannerService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<BannerListResponse> getList(@ModelAttribute BannerListRequest req) {
        return ResponseEntity.ok(adminBannerService.getBannerList(req));
    }

    @PostMapping("/save")
    public ResponseEntity<BaseSimpleResDto> save(@Valid @RequestBody BannerSaveRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminBannerService.saveBanner(req);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @PatchMapping("/active/{no}")
    public ResponseEntity<BaseSimpleResDto> updateActive(@PathVariable("no") Long bannerNo, @RequestParam("isActive") String isActive) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminBannerService.updateActive(bannerNo, isActive);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long bannerNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminBannerService.deleteBanner(bannerNo);
        return ResponseEntity.ok().build();
    }
}
