package com.section.admin.settings.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.settings.req.AdminSystemSettingHistoryListRequest;
import com.section.admin.settings.res.AdminSystemSettingHistoryDetailResponse;
import com.section.admin.settings.req.AdminSystemSettingSaveRequest;
import com.section.admin.settings.res.AdminSystemSettingHistoryListResponse;
import com.section.admin.settings.res.AdminSystemSettingResponse;
import com.section.admin.settings.service.AdminSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settings")
public class AdminSettingsRestController {

    private final AdminSettingsService adminSettingsService;

    @GetMapping("/system")
    public ResponseEntity<AdminSystemSettingResponse> getSystemSettings() {
        return ResponseEntity.ok(adminSettingsService.getSystemSettings());
    }

    @GetMapping("/system/history")
    public ResponseEntity<AdminSystemSettingHistoryListResponse> getSystemSettingHistory(
            AdminSystemSettingHistoryListRequest req,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(adminSettingsService.getSystemSettingHistory(req, page, size));
    }

    @GetMapping("/system/history/get")
    public ResponseEntity<AdminSystemSettingHistoryDetailResponse> getSystemSettingHistoryDetail(@RequestParam("historyNo") Long historyNo) {
        return ResponseEntity.ok(adminSettingsService.getSystemSettingHistoryDetail(historyNo));
    }

    @PostMapping("/system")
    public ResponseEntity<BaseSimpleResDto> saveSystemSettings(@Valid @RequestBody AdminSystemSettingSaveRequest req) {
        adminSettingsService.saveSystemSettings(req);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }
}
