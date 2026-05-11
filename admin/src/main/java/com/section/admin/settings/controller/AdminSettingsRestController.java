package com.section.admin.settings.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.settings.req.AdminSystemSettingSaveRequest;
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

    @PostMapping("/system")
    public ResponseEntity<BaseSimpleResDto> saveSystemSettings(@Valid @RequestBody AdminSystemSettingSaveRequest req) {
        adminSettingsService.saveSystemSettings(req);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }
}
