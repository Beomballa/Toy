package com.section.admin.log.controller;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.admin.log.res.AdminLogDetailResponse;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/logs")
public class AdminLogRestController {

    private final AdminLogService adminLogService;

    @GetMapping("/list")
    public ResponseEntity<AdminLogListResponse> getLogList(@ModelAttribute AdminLogListRequest req, Pageable pageable) {
        return ResponseEntity.ok(adminLogService.getLogList(req, pageable));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLogList(@ModelAttribute AdminLogListRequest req) {
        String fileName = "admin-logs-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(adminLogService.exportLogListCsv(req));
    }

    @GetMapping("/get")
    public ResponseEntity<AdminLogDetailResponse> getLogDetail(@RequestParam("no") Long logNo) {
        return ResponseEntity.ok(adminLogService.getLogDetail(logNo));
    }
}
