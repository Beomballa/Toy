package com.section.admin.task.service;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.admin.task.req.AdminOperationTaskBulkOperateRequest;
import com.section.admin.task.req.AdminOperationTaskBulkDuplicateRequest;
import com.section.admin.task.req.AdminOperationTaskBulkDeleteRequest;
import com.section.admin.task.req.AdminOperationTaskCommentSaveRequest;
import com.section.admin.task.req.AdminOperationTaskListRequest;
import com.section.admin.task.req.AdminOperationTaskSaveRequest;
import com.section.admin.task.res.AdminOperationTaskDetailResponse;
import com.section.admin.task.res.AdminOperationTaskListResponse;
import com.section.admin.task.support.AdminOperationTaskExportCsvWriter;
import com.section.admin.task.support.AdminOperationTaskExportSummary;
import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminOperationTaskListQuery;
import com.section.common.system.dto.AdminOperationTaskListResDto;
import com.section.common.system.dto.AdminOperationTaskAssigneeRecommendationDto;
import com.section.common.system.dto.AdminOperationTaskCommentCountDto;
import com.section.common.system.dto.AdminOperationTaskCommentSummaryDto;
import com.section.common.system.dto.AdminOperationTaskSummaryDto;
import com.section.common.system.entity.AdminOperationTaskComment;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminOperationTaskCommentRepository;
import com.section.common.system.repository.AdminOperationTaskRepository;
import com.section.common.system.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOperationTaskService {
    private static final int TASK_EXPORT_MAX_SIZE = 1000;

    private final AdminOperationTaskRepository adminOperationTaskRepository;
    private final AdminOperationTaskCommentRepository adminOperationTaskCommentRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminLogService adminLogService;

    public AdminOperationTaskListResponse getTaskList(AdminOperationTaskListRequest req) {
        AdminOperationTaskListQuery query = req.toQuery();
        Page<AdminOperationTaskListResDto> page = adminOperationTaskRepository.getTaskList(
                query,
                PageRequest.of(req.normalizedPage(), req.normalizedSize())
        );
        Page<AdminOperationTaskListResDto> enrichedPage = new PageImpl<>(
                enrichTaskListRows(page.getContent()),
                page.getPageable(),
                page.getTotalElements()
        );
        AdminOperationTaskSummaryDto summary = adminOperationTaskRepository.getTaskSummary(query, LocalDate.now());
        return AdminOperationTaskListResponse.of(enrichedPage, query, summary, getAssigneeOptions(), LocalDate.now());
    }

    public List<AdminOperationTask> getDashboardTasks(int limit) {
        return adminOperationTaskRepository.getDashboardTasks(LocalDate.now(), limit);
    }

    public byte[] exportTaskListCsv(AdminOperationTaskListRequest req) {
        AdminOperationTaskListQuery query = req.toQuery();
        Page<AdminOperationTaskListResDto> page = adminOperationTaskRepository.getTaskList(
                query,
                PageRequest.of(0, TASK_EXPORT_MAX_SIZE)
        );
        List<AdminOperationTaskListResDto> enrichedRows = enrichTaskListRows(page.getContent());
        Map<Long, String> assigneeNameMap = adminUserRepository.findAllById(
                        enrichedRows.stream()
                                .map(AdminOperationTaskListResDto::getAssigneeAdminNo)
                                .filter(java.util.Objects::nonNull)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(AdminUser::getAdminNo, AdminUser::getName));

        return AdminOperationTaskExportCsvWriter.write(
                AdminOperationTaskExportSummary.from(query, assigneeNameMap),
                enrichedRows,
                LocalDate.now()
        );
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
                getTaskDetailAssigneeOptions(),
                adminOperationTaskRepository.getTaskAssignmentRecommendations(LocalDate.now(), task.getAssigneeAdminNo(), 3),
                recentLogs.items(),
                adminOperationTaskCommentRepository.getTaskComments(taskNo, 20)
        );
    }

    public List<AdminOperationTaskListResponse.AssigneeOption> getAssigneeOptions() {
        return adminUserRepository.findAllByStatusOrderByNameAsc("ACTIVE").stream()
                .map(admin -> new AdminOperationTaskListResponse.AssigneeOption(admin.getAdminNo(), admin.getName()))
                .toList();
    }

    private List<AdminOperationTaskDetailResponse.AssigneeOption> getTaskDetailAssigneeOptions() {
        return adminUserRepository.findAllByStatusOrderByNameAsc("ACTIVE").stream()
                .map(admin -> new AdminOperationTaskDetailResponse.AssigneeOption(admin.getAdminNo(), admin.getName()))
                .toList();
    }

    private List<AdminOperationTaskListResDto> enrichTaskListRows(
            List<AdminOperationTaskListResDto> rows
    ) {
        if (rows == null || rows.isEmpty()) {
            return rows == null ? List.of() : rows;
        }

        List<Long> taskNos = rows.stream()
                .map(AdminOperationTaskListResDto::getTaskNo)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, AdminOperationTaskCommentSummaryDto> latestCommentMap = adminOperationTaskCommentRepository.getLatestCommentsByTaskNos(taskNos)
                .stream()
                .collect(Collectors.toMap(AdminOperationTaskCommentSummaryDto::getTaskNo, item -> item));
        Map<Long, Long> commentCountMap = adminOperationTaskCommentRepository.getCommentCountsByTaskNos(taskNos)
                .stream()
                .collect(Collectors.toMap(AdminOperationTaskCommentCountDto::getTaskNo, AdminOperationTaskCommentCountDto::getCommentCount));

        rows.forEach(row -> {
            AdminOperationTaskCommentSummaryDto latestComment = latestCommentMap.get(row.getTaskNo());
            row.setLatestCommentContent(latestComment == null ? null : latestComment.getContent());
            row.setLatestCommentAdminName(latestComment == null ? null : latestComment.getAdminName());
            row.setLatestCommentDtm(latestComment == null ? null : latestComment.getCrtDtm());
            row.setCommentCount(commentCountMap.getOrDefault(row.getTaskNo(), 0L));
        });
        return rows;
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
        AdminOperationTask task = getTask(taskNo);
        adminOperationTaskCommentRepository.deleteByTaskNo(taskNo);
        adminOperationTaskRepository.delete(task);
        adminLogService.recordCurrentAdminLog("TASK_DELETE", taskNo);
    }

    @Transactional
    public DuplicateTaskResult duplicateTask(Long taskNo) {
        AdminOperationTask source = getTask(taskNo);
        AdminOperationTask duplicated = saveDuplicatedTask(source, LocalDate.now());
        copyTaskComments(source, duplicated, adminOperationTaskCommentRepository.findByTaskNoOrderByCommentNoAsc(source.getTaskNo()));
        adminLogService.recordCurrentAdminLog("TASK_DUPLICATE", duplicated.getTaskNo());
        return new DuplicateTaskResult(duplicated.getTaskNo());
    }

    @Transactional
    public BulkDuplicateResult bulkDuplicate(AdminOperationTaskBulkDuplicateRequest req) {
        List<Long> targetTaskNos = req.normalizedTaskNos();
        List<AdminOperationTask> tasks = adminOperationTaskRepository.findAllById(targetTaskNos);
        if (tasks.isEmpty()) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Map<Long, AdminOperationTask> taskMap = tasks.stream()
                .collect(Collectors.toMap(AdminOperationTask::getTaskNo, task -> task));
        LocalDate today = LocalDate.now();
        Map<Long, List<AdminOperationTaskComment>> commentsByTaskNo = groupCommentsByTaskNo(taskMap.keySet());
        List<AdminOperationTask> duplicatedTasks = targetTaskNos.stream()
                .map(taskMap::get)
                .filter(Objects::nonNull)
                .map(sourceTask -> {
                    AdminOperationTask duplicatedTask = saveDuplicatedTask(sourceTask, today);
                    copyTaskComments(sourceTask, duplicatedTask, commentsByTaskNo.get(sourceTask.getTaskNo()));
                    return duplicatedTask;
                })
                .toList();
        duplicatedTasks.forEach(task -> adminLogService.recordCurrentAdminLog("TASK_BULK_DUPLICATE", task.getTaskNo()));

        HashSet<Long> existingTaskNoSet = new HashSet<>(taskMap.keySet());
        long missingCount = targetTaskNos.stream()
                .filter(taskNo -> !existingTaskNoSet.contains(taskNo))
                .count();

        return new BulkDuplicateResult(
                targetTaskNos.size(),
                duplicatedTasks.size(),
                (int) missingCount,
                duplicatedTasks.stream().map(AdminOperationTask::getTaskNo).toList()
        );
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
    public void updateComment(Long taskNo, Long commentNo, AdminOperationTaskCommentSaveRequest req) {
        getTask(taskNo);
        AdminOperationTaskComment comment = adminOperationTaskCommentRepository.findById(commentNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        if (!taskNo.equals(comment.getTaskNo())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        comment.updateContent(normalizeCommentText(req.content(), 2000));
        adminLogService.recordCurrentAdminLog("TASK_COMMENT_UPDATE", taskNo);
    }

    @Transactional
    public BulkOperateResult bulkOperate(AdminOperationTaskBulkOperateRequest req) {
        req.validateOperation();
        List<Long> targetTaskNos = req.normalizedTaskNos();
        String normalizedStatus = req.normalizedStatus();
        String normalizedPriority = req.normalizedPriority();
        boolean hasAssigneeChange = req.hasAssigneeChange();
        Long normalizedAssigneeAdminNo = req.normalizedAssigneeAdminNo() == null ? null : normalizeAssigneeAdminNo(req.normalizedAssigneeAdminNo());
        String normalizedPinned = req.normalizedIsPinned();
        boolean hasDueDateChange = req.hasDueDateChange();
        LocalDate normalizedDueDate = "CLEAR".equals(req.normalizedDueDateMode()) ? null : req.normalizedDueDate();

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
            if (hasAssigneeChange && !java.util.Objects.equals(normalizedAssigneeAdminNo, task.getAssigneeAdminNo())) {
                changed = true;
            }
            if (hasDueDateChange && !Objects.equals(normalizedDueDate, task.getDueDate())) {
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
                    hasAssigneeChange ? normalizedAssigneeAdminNo : task.getAssigneeAdminNo(),
                    hasDueDateChange ? normalizedDueDate : task.getDueDate(),
                    normalizedPinned == null ? task.getIsPinned() : normalizedPinned
            );
            adminLogService.recordCurrentAdminLog("TASK_BULK_UPDATE", task.getTaskNo());
            updatedCount += 1;
        }

        return new BulkOperateResult(targetTaskNos.size(), updatedCount, unchangedCount);
    }

    @Transactional
    public BulkDeleteResult bulkDelete(AdminOperationTaskBulkDeleteRequest req) {
        List<Long> targetTaskNos = req.normalizedTaskNos();
        List<AdminOperationTask> tasks = adminOperationTaskRepository.findAllById(targetTaskNos);
        if (tasks.isEmpty()) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }

        List<Long> existingTaskNos = tasks.stream()
                .map(AdminOperationTask::getTaskNo)
                .toList();
        adminOperationTaskCommentRepository.deleteByTaskNoIn(existingTaskNos);
        adminOperationTaskRepository.deleteAll(tasks);
        tasks.forEach(task -> adminLogService.recordCurrentAdminLog("TASK_BULK_DELETE", task.getTaskNo()));

        HashSet<Long> existingTaskNoSet = new HashSet<>(existingTaskNos);
        long missingCount = targetTaskNos.stream()
                .filter(taskNo -> !existingTaskNoSet.contains(taskNo))
                .count();
        return new BulkDeleteResult(targetTaskNos.size(), tasks.size(), (int) missingCount);
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

    private String buildDuplicateTitle(String title) {
        String baseTitle = normalizeRequiredText(title, 200);
        String suffix = " (복제)";
        if (baseTitle.length() + suffix.length() <= 200) {
            return baseTitle + suffix;
        }
        return baseTitle.substring(0, 200 - suffix.length()).trim() + suffix;
    }

    private AdminOperationTask saveDuplicatedTask(AdminOperationTask source, LocalDate today) {
        return adminOperationTaskRepository.save(AdminOperationTask.builder()
                .title(buildDuplicateTitle(source.getTitle()))
                .description(source.getDescription())
                .status(AdminOperationTaskStatus.TODO.name())
                .priority(source.getPriority())
                .assigneeAdminNo(source.getAssigneeAdminNo())
                // 이미 지난 마감일을 그대로 복제하면 신규 작업이 즉시 지연 상태가 되어 운영 노이즈가 커집니다.
                .dueDate(resolveDuplicatedDueDate(source.getDueDate(), today))
                .isPinned(source.getIsPinned())
                .build());
    }

    private LocalDate resolveDuplicatedDueDate(LocalDate dueDate, LocalDate today) {
        if (dueDate == null || !dueDate.isBefore(today)) {
            return dueDate;
        }
        return null;
    }

    private Map<Long, List<AdminOperationTaskComment>> groupCommentsByTaskNo(Collection<Long> taskNos) {
        if (taskNos == null || taskNos.isEmpty()) {
            return Map.of();
        }
        return adminOperationTaskCommentRepository.findByTaskNoInOrderByTaskNoAscCommentNoAsc(taskNos).stream()
                .collect(Collectors.groupingBy(
                        AdminOperationTaskComment::getTaskNo,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private void copyTaskComments(AdminOperationTask source, AdminOperationTask duplicated, List<AdminOperationTaskComment> sourceComments) {
        if (sourceComments == null || sourceComments.isEmpty()) {
            return;
        }

        List<AdminOperationTaskComment> duplicatedComments = new ArrayList<>(sourceComments.size() + 1);
        duplicatedComments.add(AdminOperationTaskComment.builder()
                .taskNo(duplicated.getTaskNo())
                .content("원본 작업 #" + source.getTaskNo() + " 메모 " + sourceComments.size() + "건을 복제했습니다.")
                .build());
        sourceComments.forEach(comment -> duplicatedComments.add(AdminOperationTaskComment.builder()
                .taskNo(duplicated.getTaskNo())
                .content(comment.getContent())
                .build()));
        adminOperationTaskCommentRepository.saveAll(duplicatedComments);
    }

    public record BulkOperateResult(
            int requestedCount,
            int updatedCount,
            int unchangedCount
    ) {
    }

    public record DuplicateTaskResult(
            Long taskNo
    ) {
    }

    public record BulkDuplicateResult(
            int requestedCount,
            int createdCount,
            int missingCount,
            List<Long> createdTaskNos
    ) {
    }

    public record BulkDeleteResult(
            int requestedCount,
            int deletedCount,
            int missingCount
    ) {
    }
}
