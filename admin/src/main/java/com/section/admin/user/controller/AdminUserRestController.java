package com.section.admin.user.controller;

import com.section.admin.user.req.AdminUserListRequest;
import com.section.admin.user.req.AdminUserSaveRequest;
import com.section.admin.user.res.AdminUserListResponse;
import com.section.admin.user.service.AdminUserService;
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
@RequestMapping("/api/admin/users")
public class AdminUserRestController {

    private final AdminUserService adminUserService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<AdminUserListResponse> getList(
            @ModelAttribute AdminUserListRequest req,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(adminUserService.getAdminList(req, page, size));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute AdminUserListRequest req) {
        String fileName = "admin-users-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(adminUserService.exportAdminListCsv(req));
    }

    @PostMapping("/save")
    public ResponseEntity<Void> save(@Valid @RequestBody AdminUserSaveRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminUserService.saveAdmin(req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long adminNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminUserService.deleteAdmin(adminNo);
        return ResponseEntity.ok().build();
    }
}
