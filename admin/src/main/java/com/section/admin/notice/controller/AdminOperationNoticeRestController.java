package com.section.admin.notice.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.notice.req.AdminOperationNoticeListRequest;
import com.section.admin.notice.req.AdminOperationNoticeSaveRequest;
import com.section.admin.notice.res.AdminOperationNoticeListResponse;
import com.section.admin.notice.service.AdminOperationNoticeService;
import com.section.admin.settings.service.AdminOperationPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settings/notices")
public class AdminOperationNoticeRestController {

    private final AdminOperationNoticeService adminOperationNoticeService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<AdminOperationNoticeListResponse> getList(@ModelAttribute AdminOperationNoticeListRequest req) {
        return ResponseEntity.ok(adminOperationNoticeService.getNoticeList(req));
    }

    @PostMapping("/save")
    public ResponseEntity<BaseSimpleResDto> save(@Valid @RequestBody AdminOperationNoticeSaveRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationNoticeService.saveNotice(req);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @PatchMapping("/active/{no}")
    public ResponseEntity<BaseSimpleResDto> updateActive(@PathVariable("no") Long noticeNo, @RequestParam("isActive") String isActive) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationNoticeService.updateActive(noticeNo, isActive);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long noticeNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationNoticeService.deleteNotice(noticeNo);
        return ResponseEntity.ok().build();
    }
}
