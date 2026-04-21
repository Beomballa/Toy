package com.section.admin.product.controller;

import com.section.admin.product.service.AdminLogService;
import com.section.common.system.entity.AdminActivityLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/logs")
public class AdminLogRestController {

    private final AdminLogService adminLogService;

    @GetMapping("/list")
    public ResponseEntity<Page<AdminActivityLog>> getLogList(Pageable pageable) {
        return ResponseEntity.ok(adminLogService.getLogList(pageable));
    }
}
