package com.section.admin.task.res;

import com.section.admin.log.res.AdminLogListResponse;
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
            AdminOperationTaskWorkloadDto workload,
            List<AdminOperationTaskListResDto> recentTasks,
            List<AdminOperationTaskListResDto> overdueTasks,
            List<AdminOperationTaskWorkloadCommentSummaryDto> recentComments,
            List<AdminLogListResponse.Item> recentHistories
    ) {
        String resolvedAssigneeName = assigneeAdminName == null || assigneeAdminName.isBlank()
                ? "관리자#" + assigneeAdminNo
                : assigneeAdminName;
        return new AdminOperationTaskWorkloadDetailResponse(
                assigneeAdminNo,
                resolvedAssigneeName,
                Summary.from(workload),
                "/admin/settings/tasks?assigneeAdminNo=" + assigneeAdminNo,
                "/admin/settings/tasks?assigneeAdminNo=" + assigneeAdminNo + "&status=TODO",
                "/admin/settings/tasks?assigneeAdminNo=" + assigneeAdminNo + "&status=IN_PROGRESS",
                "/admin/settings/tasks?assigneeAdminNo=" + assigneeAdminNo + "&overdueOnly=Y",
                "/admin/settings/logs?adminNo=" + assigneeAdminNo + "&actionType=TASK_",
                recentTasks == null ? List.of() : recentTasks.stream().map(RecentTask::from).toList(),
                overdueTasks == null ? List.of() : overdueTasks.stream().map(RecentTask::from).toList(),
                recentComments == null ? List.of() : recentComments.stream().map(RecentComment::from).toList(),
                recentHistories == null ? List.of() : recentHistories.stream().map(RecentHistory::from).toList()
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
        static RecentTask from(AdminOperationTaskListResDto item) {
            return new RecentTask(
                    item.getTaskNo(),
                    item.getTitle(),
                    AdminOperationTaskStatus.fromCode(item.getStatus()).getLabel(),
                    AdminOperationTaskPriority.fromCode(item.getPriority()).getLabel(),
                    resolveDueState(item, LocalDate.now()),
                    "/admin/settings/tasks/get?no=" + item.getTaskNo() + "&returnTo=" + encode("/admin/settings/tasks/workloads/get?adminNo=" + item.getAssigneeAdminNo()),
                    "/admin/settings/tasks/history?taskNo=" + item.getTaskNo() + "&returnTo=" + encode("/admin/settings/tasks/workloads/get?adminNo=" + item.getAssigneeAdminNo())
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
        static RecentComment from(AdminOperationTaskWorkloadCommentSummaryDto item) {
            return new RecentComment(
                    item.getCommentNo(),
                    item.getTaskNo(),
                    item.getTaskTitle(),
                    resolveAdminName(item),
                    item.getContent(),
                    format(item.getCrtDtm()),
                    "/admin/settings/tasks/get?no=" + item.getTaskNo() + "&returnTo=" + encode("/admin/settings/tasks/workloads/get?adminNo=" + item.getAssigneeAdminNo())
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
        static RecentHistory from(AdminLogListResponse.Item item) {
            Long taskNo = item.targetId();
            return new RecentHistory(
                    item.logNo(),
                    taskNo,
                    item.targetLabel(),
                    resolveActionLabel(item.actionType()),
                    item.adminName(),
                    item.actionDtm(),
                    taskNo == null ? null : "/admin/settings/tasks/get?no=" + taskNo + "&returnTo=" + encode("/admin/settings/tasks/workloads/get?adminNo=" + item.adminNo()),
                    "/api/admin/logs/get?no=" + item.logNo()
            );
        }

        private static String resolveActionLabel(String actionType) {
            return switch (actionType) {
                case "TASK_CREATE" -> "작업 생성";
                case "TASK_UPDATE" -> "작업 수정";
                case "TASK_STATUS_UPDATE" -> "상태 변경";
                case "TASK_BULK_UPDATE" -> "일괄 변경";
                case "TASK_COMMENT_CREATE" -> "댓글 등록";
                case "TASK_COMMENT_DELETE" -> "댓글 삭제";
                case "TASK_DELETE" -> "작업 삭제";
                default -> actionType == null ? "-" : actionType;
            };
        }
    }

    private static String format(LocalDateTime value) {
        return value == null ? "-" : value.toString().replace('T', ' ');
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
