package com.section.admin.dashboard.controller;

import com.section.admin.dashboard.res.DashboardResponse;
import com.section.admin.dashboard.service.AdminDashBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashBoardRestController {

    private final AdminDashBoardService adminDashBoardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(adminDashBoardService.getDashboardData());
    }
}
