package com.section.admin.task.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.admin.task.req.AdminOperationTaskListRequest;
import com.section.admin.task.req.AdminOperationTaskSaveRequest;
import com.section.admin.task.res.AdminOperationTaskDetailResponse;
import com.section.admin.task.res.AdminOperationTaskListResponse;
import com.section.admin.task.service.AdminOperationTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settings/tasks")
public class AdminOperationTaskRestController {

    private final AdminOperationTaskService adminOperationTaskService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<AdminOperationTaskListResponse> getList(@ModelAttribute AdminOperationTaskListRequest req) {
        return ResponseEntity.ok(adminOperationTaskService.getTaskList(req));
    }

    @GetMapping("/{no}")
    public ResponseEntity<AdminOperationTaskDetailResponse> getDetail(@PathVariable("no") Long taskNo) {
        return ResponseEntity.ok(adminOperationTaskService.getTaskDetail(taskNo));
    }

    @PostMapping("/save")
    public ResponseEntity<BaseSimpleResDto> save(@Valid @RequestBody AdminOperationTaskSaveRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationTaskService.saveTask(req);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @PatchMapping("/status/{no}")
    public ResponseEntity<BaseSimpleResDto> updateStatus(@PathVariable("no") Long taskNo, @RequestParam("status") String status) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationTaskService.updateStatus(taskNo, status);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long taskNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationTaskService.deleteTask(taskNo);
        return ResponseEntity.ok().build();
    }
}
