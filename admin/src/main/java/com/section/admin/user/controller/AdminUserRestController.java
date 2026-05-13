package com.section.admin.user.controller;

import com.section.admin.user.req.AdminUserSaveRequest;
import com.section.admin.user.res.AdminUserListResponse;
import com.section.admin.user.service.AdminUserService;
import com.section.admin.settings.service.AdminOperationPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserRestController {

    private final AdminUserService adminUserService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<List<AdminUserListResponse>> getList() {
        return ResponseEntity.ok(adminUserService.getAdminList());
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
