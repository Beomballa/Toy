package com.section.admin.banner.controller;

import com.section.admin.banner.req.BannerBulkDeleteRequest;
import com.section.admin.banner.req.BannerBulkOperateRequest;
import com.section.admin.banner.req.BannerListRequest;
import com.section.admin.banner.req.BannerSaveRequest;
import com.section.admin.banner.res.BannerDetailResponse;
import com.section.admin.banner.res.BannerListResponse;
import com.section.admin.banner.service.AdminBannerService;
import com.section.admin.base.res.BaseSimpleResDto;
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
@RequestMapping("/api/admin/banners")
public class AdminBannerRestController {

    private final AdminBannerService adminBannerService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<BannerListResponse> getList(@ModelAttribute BannerListRequest req) {
        return ResponseEntity.ok(adminBannerService.getBannerList(req));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute BannerListRequest req) {
        String fileName = "banners-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(adminBannerService.exportBannerListCsv(req));
    }

    @GetMapping("/{no}")
    public ResponseEntity<BannerDetailResponse> getDetail(@PathVariable("no") Long bannerNo) {
        return ResponseEntity.ok(adminBannerService.getBannerDetail(bannerNo));
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

    @PatchMapping("/bulk-operate")
    public ResponseEntity<AdminBannerService.BulkOperateResult> bulkOperate(@RequestBody BannerBulkOperateRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminBannerService.bulkOperate(req));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long bannerNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminBannerService.deleteBanner(bannerNo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-delete")
    public ResponseEntity<AdminBannerService.BulkDeleteResult> bulkDelete(@RequestBody BannerBulkDeleteRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminBannerService.bulkDelete(req));
    }
}
