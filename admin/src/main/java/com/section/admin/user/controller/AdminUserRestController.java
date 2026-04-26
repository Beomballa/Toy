package com.section.admin.user.controller;

import com.section.admin.user.service.AdminUserService;
import com.section.common.system.entity.AdminUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserRestController {

    private final AdminUserService adminUserService;

    @GetMapping("/list")
    public ResponseEntity<List<AdminUser>> getList() {
        return ResponseEntity.ok(adminUserService.getAdminList());
    }

    @PostMapping("/save")
    public ResponseEntity<Void> save(@RequestBody AdminUser adminUser) {
        adminUserService.saveAdmin(adminUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long adminNo) {
        adminUserService.deleteAdmin(adminNo);
        return ResponseEntity.ok().build();
    }
}
