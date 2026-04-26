package com.section.admin.user.controller;

import com.section.common.system.entity.Account;
import com.section.common.system.service.AdminAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminMemberRestController {

    private final AdminAccountService adminAccountService;

    @GetMapping("/list")
    public ResponseEntity<Page<Account>> getList(Pageable pageable) {
        return ResponseEntity.ok(adminAccountService.getAccountList(pageable));
    }
}
