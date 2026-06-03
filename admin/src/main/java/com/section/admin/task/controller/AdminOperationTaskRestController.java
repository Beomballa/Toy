package com.section.admin.task.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.admin.task.req.AdminOperationTaskBulkOperateRequest;
import com.section.admin.task.req.AdminOperationTaskBulkDeleteRequest;
import com.section.admin.task.req.AdminOperationTaskBulkDuplicateRequest;
import com.section.admin.task.req.AdminOperationTaskCommentSaveRequest;
import com.section.admin.task.req.AdminOperationTaskHistoryListRequest;
import com.section.admin.task.req.AdminOperationTaskListRequest;
import com.section.admin.task.req.AdminOperationTaskSaveRequest;
import com.section.admin.task.req.AdminOperationTaskWorkloadListRequest;
import com.section.admin.task.res.AdminOperationTaskDetailResponse;
import com.section.admin.task.res.AdminOperationTaskHistoryListResponse;
import com.section.admin.task.res.AdminOperationTaskListResponse;
import com.section.admin.task.res.AdminOperationTaskWorkloadDetailResponse;
import com.section.admin.task.res.AdminOperationTaskWorkloadListResponse;
import com.section.admin.task.service.AdminOperationTaskHistoryService;
import com.section.admin.task.service.AdminOperationTaskService;
import com.section.admin.task.service.AdminOperationTaskWorkloadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settings/tasks")
public class AdminOperationTaskRestController {

    private final AdminOperationTaskService adminOperationTaskService;
    private final AdminOperationTaskHistoryService adminOperationTaskHistoryService;
    private final AdminOperationTaskWorkloadService adminOperationTaskWorkloadService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<AdminOperationTaskListResponse> getList(@ModelAttribute AdminOperationTaskListRequest req) {
        return ResponseEntity.ok(adminOperationTaskService.getTaskList(req));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute AdminOperationTaskListRequest req) {
        String fileName = "tasks-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(adminOperationTaskService.exportTaskListCsv(req));
    }

    @GetMapping("/workloads/list")
    public ResponseEntity<AdminOperationTaskWorkloadListResponse> getWorkloads(@ModelAttribute AdminOperationTaskWorkloadListRequest req) {
        return ResponseEntity.ok(adminOperationTaskWorkloadService.getWorkloadList(req));
    }

    @GetMapping("/workloads/{adminNo}")
    public ResponseEntity<AdminOperationTaskWorkloadDetailResponse> getWorkloadDetail(@PathVariable Long adminNo) {
        return ResponseEntity.ok(adminOperationTaskWorkloadService.getWorkloadDetail(adminNo));
    }

    @GetMapping("/{no}")
    public ResponseEntity<AdminOperationTaskDetailResponse> getDetail(@PathVariable("no") Long taskNo) {
        return ResponseEntity.ok(adminOperationTaskService.getTaskDetail(taskNo));
    }

    @GetMapping("/history/list")
    public ResponseEntity<AdminOperationTaskHistoryListResponse> getHistoryList(
            @ModelAttribute AdminOperationTaskHistoryListRequest req,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(adminOperationTaskHistoryService.getTaskHistoryList(req, page, size));
    }

    @GetMapping("/history/export")
    public ResponseEntity<byte[]> exportHistory(@ModelAttribute AdminOperationTaskHistoryListRequest req) {
        String fileName = "task-history-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(adminOperationTaskHistoryService.exportTaskHistoryCsv(req));
    }

    @PostMapping("/save")
    public ResponseEntity<BaseSimpleResDto> save(@Valid @RequestBody AdminOperationTaskSaveRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationTaskService.saveTask(req);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @PostMapping("/{no}/comments")
    public ResponseEntity<BaseSimpleResDto> addComment(@PathVariable("no") Long taskNo,
                                                       @Valid @RequestBody AdminOperationTaskCommentSaveRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationTaskService.addComment(taskNo, req);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @PatchMapping("/status/{no}")
    public ResponseEntity<BaseSimpleResDto> updateStatus(@PathVariable("no") Long taskNo, @RequestParam("status") String status) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationTaskService.updateStatus(taskNo, status);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @PostMapping("/{no}/duplicate")
    public ResponseEntity<AdminOperationTaskService.DuplicateTaskResult> duplicate(@PathVariable("no") Long taskNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminOperationTaskService.duplicateTask(taskNo));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long taskNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationTaskService.deleteTask(taskNo);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{taskNo}/comments/{commentNo}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long taskNo, @PathVariable Long commentNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationTaskService.deleteComment(taskNo, commentNo);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{taskNo}/comments/{commentNo}")
    public ResponseEntity<BaseSimpleResDto> updateComment(@PathVariable Long taskNo,
                                                          @PathVariable Long commentNo,
                                                          @Valid @RequestBody AdminOperationTaskCommentSaveRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminOperationTaskService.updateComment(taskNo, commentNo, req);
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @PostMapping("/bulk-operate")
    public ResponseEntity<AdminOperationTaskService.BulkOperateResult> bulkOperate(@RequestBody AdminOperationTaskBulkOperateRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminOperationTaskService.bulkOperate(req));
    }

    @PostMapping("/bulk-duplicate")
    public ResponseEntity<AdminOperationTaskService.BulkDuplicateResult> bulkDuplicate(@RequestBody AdminOperationTaskBulkDuplicateRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminOperationTaskService.bulkDuplicate(req));
    }

    @PostMapping("/bulk-delete")
    public ResponseEntity<AdminOperationTaskService.BulkDeleteResult> bulkDelete(@RequestBody AdminOperationTaskBulkDeleteRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        return ResponseEntity.ok(adminOperationTaskService.bulkDelete(req));
    }
}
