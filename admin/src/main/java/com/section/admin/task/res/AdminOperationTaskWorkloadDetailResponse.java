package com.section.admin.task.res;

import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.task.support.AdminTaskLinkSupport;
import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.system.dto.AdminOperationTaskListResDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadCommentSummaryDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOperationTaskWorkloadDetailResponse(
        Long assigneeAdminNo,
        String assigneeAdminName,
        Summary summary,
        String targetPath,
        String todoPath,
        String inProgressPath,
        String overduePath,
        String activityLogPath,
        List<RecentTask> recentTasks,
        List<RecentTask> overdueTasks,
        List<RecentComment> recentComments,
        List<RecentHistory> recentHistories
) {
    public static AdminOperationTaskWorkloadDetailResponse of(
            Long assigneeAdminNo,
            String assigneeAdminName,
            String returnTo,
            AdminOperationTaskWorkloadDto workload,
            List<AdminOperationTaskListResDto> recentTasks,
            List<AdminOperationTaskListResDto> overdueTasks,
            List<AdminOperationTaskWorkloadCommentSummaryDto> recentComments,
            List<AdminLogListResponse.Item> recentHistories
    ) {
        String resolvedAssigneeName = assigneeAdminName == null || assigneeAdminName.isBlank()
                ? "관리자#" + assigneeAdminNo
                : assigneeAdminName;
        String detailReturnTo = buildDetailReturnTo(assigneeAdminNo, returnTo);
        String taskListReturnTo = encode(detailReturnTo);
        return new AdminOperationTaskWorkloadDetailResponse(
                assigneeAdminNo,
                resolvedAssigneeName,
                Summary.from(workload),
                buildTaskListPath(assigneeAdminNo, null, false, taskListReturnTo),
                buildTaskListPath(assigneeAdminNo, "TODO", false, taskListReturnTo),
                buildTaskListPath(assigneeAdminNo, "IN_PROGRESS", false, taskListReturnTo),
                buildTaskListPath(assigneeAdminNo, null, true, taskListReturnTo),
                "/admin/settings/logs?adminNo=" + assigneeAdminNo + "&actionType=TASK_",
                recentTasks == null ? List.of() : recentTasks.stream().map(item -> RecentTask.from(item, detailReturnTo)).toList(),
                overdueTasks == null ? List.of() : overdueTasks.stream().map(item -> RecentTask.from(item, detailReturnTo)).toList(),
                recentComments == null ? List.of() : recentComments.stream().map(item -> RecentComment.from(item, detailReturnTo)).toList(),
                recentHistories == null ? List.of() : recentHistories.stream().map(item -> RecentHistory.from(item, detailReturnTo)).toList()
        );
    }

    public record Summary(
            long totalCount,
            long todoCount,
            long inProgressCount,
            long overdueCount
    ) {
        static Summary from(AdminOperationTaskWorkloadDto workload) {
            return new Summary(
                    workload == null ? 0L : workload.totalCount(),
                    workload == null ? 0L : workload.todoCount(),
                    workload == null ? 0L : workload.inProgressCount(),
                    workload == null ? 0L : workload.overdueCount()
            );
        }
    }

    public record RecentTask(
            Long taskNo,
            String title,
            String statusLabel,
            String priorityLabel,
            String dueState,
            String taskPath,
            String historyPath
    ) {
        static RecentTask from(AdminOperationTaskListResDto item, String detailReturnTo) {
            return new RecentTask(
                    item.getTaskNo(),
                    item.getTitle(),
                    AdminOperationTaskStatus.fromCode(item.getStatus()).getLabel(),
                    AdminOperationTaskPriority.fromCode(item.getPriority()).getLabel(),
                    resolveDueState(item, LocalDate.now()),
                    AdminTaskLinkSupport.buildListOpenPath(item.getTaskNo(), detailReturnTo, "task-workload-detail"),
                    "/admin/settings/tasks/history?taskNo=" + item.getTaskNo() + "&returnTo=" + encode(detailReturnTo)
            );
        }

        private static String resolveDueState(AdminOperationTaskListResDto item, LocalDate today) {
            if (item.getDueDate() == null) {
                return "기한 없음";
            }
            if ("DONE".equalsIgnoreCase(item.getStatus())) {
                return "완료";
            }
            if (item.getDueDate().isBefore(today)) {
                return "기한 초과";
            }
            if (item.getDueDate().isEqual(today)) {
                return "오늘 마감";
            }
            return item.getDueDate().toString();
        }
    }

    public record RecentComment(
            Long commentNo,
            Long taskNo,
            String taskTitle,
            String adminName,
            String content,
            String commentDtm,
            String taskPath
    ) {
        static RecentComment from(AdminOperationTaskWorkloadCommentSummaryDto item, String detailReturnTo) {
            return new RecentComment(
                    item.getCommentNo(),
                    item.getTaskNo(),
                    item.getTaskTitle(),
                    resolveAdminName(item),
                    item.getContent(),
                    format(item.getCrtDtm()),
                    AdminTaskLinkSupport.buildListOpenPath(item.getTaskNo(), detailReturnTo, "task-workload-detail")
            );
        }

        private static String resolveAdminName(AdminOperationTaskWorkloadCommentSummaryDto item) {
            if (item.getAdminName() != null && !item.getAdminName().isBlank()) {
                return item.getAdminName();
            }
            return item.getAdminNo() == null ? "관리자" : "관리자#" + item.getAdminNo();
        }
    }

    public record RecentHistory(
            Long logNo,
            Long taskNo,
            String taskLabel,
            String actionLabel,
            String adminName,
            String actionDtm,
            String taskPath,
            String logDetailPath
    ) {
        static RecentHistory from(AdminLogListResponse.Item item, String detailReturnTo) {
            Long taskNo = item.targetId();
            return new RecentHistory(
                    item.logNo(),
                    taskNo,
                    item.targetLabel(),
                    resolveActionLabel(item.actionType()),
                    item.adminName(),
                    item.actionDtm(),
                    taskNo == null ? null : AdminTaskLinkSupport.buildListOpenPath(taskNo, detailReturnTo, "task-workload-detail"),
                    "/api/admin/logs/get?no=" + item.logNo()
            );
        }

        private static String resolveActionLabel(String actionType) {
            return switch (actionType) {
                case "TASK_CREATE" -> "작업 생성";
                case "TASK_UPDATE" -> "작업 수정";
                case "TASK_STATUS_UPDATE" -> "상태 변경";
                case "TASK_DUPLICATE" -> "작업 복제";
                case "TASK_BULK_UPDATE" -> "일괄 변경";
                case "TASK_BULK_DUPLICATE" -> "일괄 복제";
                case "TASK_COMMENT_CREATE" -> "댓글 등록";
                case "TASK_COMMENT_UPDATE" -> "댓글 수정";
                case "TASK_COMMENT_DELETE" -> "댓글 삭제";
                case "TASK_BULK_DELETE" -> "일괄 삭제";
                case "TASK_DELETE" -> "작업 삭제";
                default -> actionType == null ? "-" : actionType;
            };
        }
    }

    private static String format(LocalDateTime value) {
        return value == null ? "-" : value.toString().replace('T', ' ');
    }

    private static String buildDetailReturnTo(Long assigneeAdminNo, String returnTo) {
        StringBuilder builder = new StringBuilder("/admin/settings/tasks/workloads/get?adminNo=").append(assigneeAdminNo);
        if (returnTo != null && !returnTo.isBlank()) {
            builder.append("&returnTo=").append(encode(returnTo));
        }
        return builder.toString();
    }

    private static String buildTaskListPath(Long assigneeAdminNo, String status, boolean overdueOnly, String returnTo) {
        StringBuilder builder = new StringBuilder("/admin/settings/tasks?assigneeAdminNo=").append(assigneeAdminNo);
        if (status != null && !status.isBlank()) {
            builder.append("&status=").append(status);
        }
        if (overdueOnly) {
            builder.append("&overdueOnly=Y");
        }
        builder.append("&returnTo=").append(returnTo);
        builder.append("&source=task-workload-detail");
        return builder.toString();
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
