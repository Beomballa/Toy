package com.section.admin.task.service;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.admin.task.req.AdminOperationTaskBulkOperateRequest;
import com.section.admin.task.req.AdminOperationTaskCommentSaveRequest;
import com.section.admin.task.req.AdminOperationTaskListRequest;
import com.section.admin.task.req.AdminOperationTaskSaveRequest;
import com.section.admin.task.res.AdminOperationTaskDetailResponse;
import com.section.admin.task.res.AdminOperationTaskListResponse;
import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminOperationTaskListQuery;
import com.section.common.system.dto.AdminOperationTaskSummaryDto;
import com.section.common.system.entity.AdminOperationTaskComment;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminOperationTaskCommentRepository;
import com.section.common.system.repository.AdminOperationTaskRepository;
import com.section.common.system.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOperationTaskService {

    private final AdminOperationTaskRepository adminOperationTaskRepository;
    private final AdminOperationTaskCommentRepository adminOperationTaskCommentRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminLogService adminLogService;

    public AdminOperationTaskListResponse getTaskList(AdminOperationTaskListRequest req) {
        AdminOperationTaskListQuery query = req.toQuery();
        Page<com.section.common.system.dto.AdminOperationTaskListResDto> page = adminOperationTaskRepository.getTaskList(
                query,
                PageRequest.of(req.normalizedPage(), req.normalizedSize())
        );
        AdminOperationTaskSummaryDto summary = adminOperationTaskRepository.getTaskSummary(query, LocalDate.now());
        return AdminOperationTaskListResponse.of(page, query, summary, getAssigneeOptions(), LocalDate.now());
    }

    public List<AdminOperationTask> getDashboardTasks(int limit) {
        return adminOperationTaskRepository.getDashboardTasks(LocalDate.now(), limit);
    }

    public AdminOperationTaskDetailResponse getTaskDetail(Long taskNo) {
        AdminOperationTask task = getTask(taskNo);
        String assigneeAdminName = resolveAssigneeAdminName(task.getAssigneeAdminNo());

        AdminLogListRequest request = new AdminLogListRequest();
        request.setTargetId(taskNo);
        request.setActionType("TASK_");
        AdminLogListResponse recentLogs = adminLogService.getLogList(request, PageRequest.of(0, 5));

        return AdminOperationTaskDetailResponse.from(
                task,
                assigneeAdminName,
                recentLogs.items(),
                adminOperationTaskCommentRepository.getTaskComments(taskNo, 20)
        );
    }

    public List<AdminOperationTaskListResponse.AssigneeOption> getAssigneeOptions() {
        return adminUserRepository.findAll().stream()
                .sorted(Comparator.comparing(AdminUser::getName))
                .map(admin -> new AdminOperationTaskListResponse.AssigneeOption(admin.getAdminNo(), admin.getName()))
                .toList();
    }

    @Transactional
    public void saveTask(AdminOperationTaskSaveRequest req) {
        String normalizedTitle = normalizeRequiredText(req.title(), 200);
        String normalizedDescription = normalizeOptionalText(req.description(), 5000);
        String normalizedStatus = normalizeStatus(req.status());
        String normalizedPriority = normalizePriority(req.priority());
        String normalizedPinned = normalizeFlag(req.isPinned(), "N");
        Long normalizedAssigneeAdminNo = normalizeAssigneeAdminNo(req.assigneeAdminNo());

        if (req.taskNo() == null) {
            AdminOperationTask saved = adminOperationTaskRepository.save(AdminOperationTask.builder()
                    .title(normalizedTitle)
                    .description(normalizedDescription)
                    .status(normalizedStatus)
                    .priority(normalizedPriority)
                    .assigneeAdminNo(normalizedAssigneeAdminNo)
                    .dueDate(req.dueDate())
                    .isPinned(normalizedPinned)
                    .build());
            adminLogService.recordCurrentAdminLog("TASK_CREATE", saved.getTaskNo());
            return;
        }

        AdminOperationTask task = getTask(req.taskNo());
        task.update(
                normalizedTitle,
                normalizedDescription,
                normalizedStatus,
                normalizedPriority,
                normalizedAssigneeAdminNo,
                req.dueDate(),
                normalizedPinned
        );
        adminLogService.recordCurrentAdminLog("TASK_UPDATE", task.getTaskNo());
    }

    @Transactional
    public void updateStatus(Long taskNo, String status) {
        AdminOperationTask task = getTask(taskNo);
        task.updateStatus(normalizeStatus(status));
        adminLogService.recordCurrentAdminLog("TASK_STATUS_UPDATE", taskNo);
    }

    @Transactional
    public void deleteTask(Long taskNo) {
        adminOperationTaskRepository.deleteById(taskNo);
        adminLogService.recordCurrentAdminLog("TASK_DELETE", taskNo);
    }

    @Transactional
    public void addComment(Long taskNo, AdminOperationTaskCommentSaveRequest req) {
        getTask(taskNo);
        String normalizedContent = normalizeCommentText(req.content(), 2000);
        adminOperationTaskCommentRepository.save(AdminOperationTaskComment.builder()
                .taskNo(taskNo)
                .content(normalizedContent)
                .build());
        adminLogService.recordCurrentAdminLog("TASK_COMMENT_CREATE", taskNo);
    }

    @Transactional
    public void deleteComment(Long taskNo, Long commentNo) {
        getTask(taskNo);
        AdminOperationTaskComment comment = adminOperationTaskCommentRepository.findById(commentNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        if (!taskNo.equals(comment.getTaskNo())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        adminOperationTaskCommentRepository.delete(comment);
        adminLogService.recordCurrentAdminLog("TASK_COMMENT_DELETE", taskNo);
    }

    @Transactional
    public BulkOperateResult bulkOperate(AdminOperationTaskBulkOperateRequest req) {
        req.validateOperation();
        List<Long> targetTaskNos = req.normalizedTaskNos();
        String normalizedStatus = req.normalizedStatus();
        String normalizedPriority = req.normalizedPriority();
        Long normalizedAssigneeAdminNo = req.normalizedAssigneeAdminNo() == null ? null : normalizeAssigneeAdminNo(req.normalizedAssigneeAdminNo());
        String normalizedPinned = req.normalizedIsPinned();

        List<AdminOperationTask> tasks = adminOperationTaskRepository.findAllById(targetTaskNos);
        if (tasks.isEmpty()) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }

        int updatedCount = 0;
        int unchangedCount = 0;
        for (AdminOperationTask task : tasks) {
            boolean changed = false;
            if (normalizedStatus != null && !normalizedStatus.equalsIgnoreCase(task.getStatus())) {
                changed = true;
            }
            if (normalizedPriority != null && !normalizedPriority.equalsIgnoreCase(task.getPriority())) {
                changed = true;
            }
            if (normalizedPinned != null && !normalizedPinned.equalsIgnoreCase(task.getIsPinned())) {
                changed = true;
            }
            if (req.normalizedAssigneeAdminNo() != null && !java.util.Objects.equals(normalizedAssigneeAdminNo, task.getAssigneeAdminNo())) {
                changed = true;
            }

            if (!changed) {
                unchangedCount += 1;
                continue;
            }

            task.update(
                    task.getTitle(),
                    task.getDescription(),
                    normalizedStatus == null ? task.getStatus() : normalizedStatus,
                    normalizedPriority == null ? task.getPriority() : normalizedPriority,
                    req.normalizedAssigneeAdminNo() == null ? task.getAssigneeAdminNo() : normalizedAssigneeAdminNo,
                    task.getDueDate(),
                    normalizedPinned == null ? task.getIsPinned() : normalizedPinned
            );
            adminLogService.recordCurrentAdminLog("TASK_BULK_UPDATE", task.getTaskNo());
            updatedCount += 1;
        }

        return new BulkOperateResult(targetTaskNos.size(), updatedCount, unchangedCount);
    }

    private AdminOperationTask getTask(Long taskNo) {
        return adminOperationTaskRepository.findById(taskNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private String resolveAssigneeAdminName(Long assigneeAdminNo) {
        if (assigneeAdminNo == null) {
            return "미지정";
        }
        return adminUserRepository.findById(assigneeAdminNo)
                .map(AdminUser::getName)
                .orElse("관리자#" + assigneeAdminNo);
    }

    private String normalizeRequiredText(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank() || normalized.length() > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeCommentText(String value, int maxLength) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String normalized = value.replace("\r\n", "\n").trim();
        if (normalized.isBlank() || normalized.length() > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private String normalizeStatus(String value) {
        try {
            return AdminOperationTaskStatus.fromCode(value).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizePriority(String value) {
        try {
            return AdminOperationTaskPriority.fromCode(value).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private Long normalizeAssigneeAdminNo(Long value) {
        if (value == null || value == 0L) {
            return null;
        }
        AdminUser adminUser = adminUserRepository.findById(value)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        return adminUser.getAdminNo();
    }

    private String normalizeFlag(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    public record BulkOperateResult(
            int requestedCount,
            int updatedCount,
            int unchangedCount
    ) {
    }
}
