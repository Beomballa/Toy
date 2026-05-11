package com.section.admin.user.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.user.req.AdminMemberListRequest;
import com.section.admin.user.req.AdminMemberStatusUpdateRequest;
import com.section.admin.user.res.AdminMemberDetailResponse;
import com.section.admin.user.res.AdminMemberListResponse;
import com.section.admin.user.service.AdminMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminMemberRestController {

    private final AdminMemberService adminMemberService;

    @GetMapping("/list")
    public ResponseEntity<AdminMemberListResponse> getList(@ModelAttribute AdminMemberListRequest req, Pageable pageable) {
        return ResponseEntity.ok(adminMemberService.getMemberList(req, pageable));
    }

    @GetMapping("/get")
    public ResponseEntity<AdminMemberDetailResponse> getDetail(@RequestParam("id") Long memberId) {
        return ResponseEntity.ok(adminMemberService.getMemberDetail(memberId));
    }

    @PatchMapping("/status/{id}")
    public ResponseEntity<BaseSimpleResDto> updateStatus(@PathVariable("id") Long memberId, @Valid @RequestBody AdminMemberStatusUpdateRequest req) {
        adminMemberService.updateMemberStatus(memberId, req);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }
}
