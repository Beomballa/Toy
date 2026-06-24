package com.section.admin.notice.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.notice.req.AdminOperationNoticeBulkDeleteRequest;
import com.section.admin.notice.req.AdminOperationNoticeBulkOperateRequest;
import com.section.admin.notice.req.AdminOperationNoticeHistoryListRequest;
import com.section.admin.notice.res.AdminOperationNoticeDetailResponse;
import com.section.admin.notice.req.AdminOperationNoticeListRequest;
import com.section.admin.notice.req.AdminOperationNoticeSaveRequest;
import com.section.admin.notice.res.AdminOperationNoticeHistoryListResponse;
import com.section.admin.notice.res.AdminOperationNoticeListResponse;
import com.section.admin.notice.res.AdminOperationNoticeSaveResponse;
import com.section.admin.notice.service.AdminOperationNoticeHistoryService;
import com.section.admin.notice.service.AdminOperationNoticeService;
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
@RequestMapping("/api/admin/settings/notices")
public class AdminOperationNoticeRestController {

    private final AdminOperationNoticeService adminOperationNoticeService;
    private final AdminOperationNoticeHistoryService adminOperationNoticeHistoryService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<AdminOperationNoticeListResponse> getList(@ModelAttribute AdminOperationNoticeListRequest req) {
        return ResponseEntity.ok(adminOperationNoticeService.getNoticeList(req));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute AdminOperationNoticeListRequest req) {
        String fileName = "notices-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(adminOperationNoticeService.exportNoticeListCsv(req));
    }

    @GetMapping("/history/list")
    public ResponseEntity<AdminOperationNoticeHistoryListResponse> getHistoryList(
            @ModelAttribute AdminOperationNoticeHistoryListRequest req,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(adminOperationNoticeHistoryService.getNoticeHistoryList(req, page, size));
    }

    @GetMapping("/history/export")
    public ResponseEntity<byte[]> exportHistory(@ModelAttribute AdminOperationNoticeHistoryListRequest req) {
        String fileName = "notice-history-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(adminOperationNoticeHistoryService.exportNoticeHistoryListCsv(req));
    }

    @GetMapping("/{no}")
    public ResponseEntity<AdminOperationNoticeDetailResponse> getDetail(@PathVariable("no") Long noticeNo) {
        return ResponseEntity.ok(adminOperationNoticeService.getNoticeDetail(noticeNo));
    }

    @PostMapping("/save")
    public ResponseEntity<AdminOperationNoticeSaveResponse> save(@Valid @RequestBody AdminOperationNoticeSaveRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(new AdminOperationNoticeSaveResponse(adminOperationNoticeService.saveNotice(req)));
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

    @PostMapping("/bulk-operate")
    public ResponseEntity<AdminOperationNoticeService.BulkOperateResult> bulkOperate(@RequestBody AdminOperationNoticeBulkOperateRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminOperationNoticeService.bulkOperate(req));
    }

    @PostMapping("/bulk-delete")
    public ResponseEntity<AdminOperationNoticeService.BulkDeleteResult> bulkDelete(@RequestBody AdminOperationNoticeBulkDeleteRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminOperationNoticeService.bulkDelete(req));
    }
}
