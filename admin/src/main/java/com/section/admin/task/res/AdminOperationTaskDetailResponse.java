package com.section.admin.task.res;

import com.section.admin.log.res.AdminLogListResponse;
import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.system.dto.AdminOperationTaskCommentResDto;
import com.section.common.system.entity.AdminOperationTask;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOperationTaskDetailResponse(
        Long taskNo,
        String title,
        String description,
        String status,
        String statusLabel,
        String priority,
        String priorityLabel,
        Long assigneeAdminNo,
        String assigneeAdminName,
        String dueDate,
        String dueState,
        String isPinned,
        String crtDtm,
        String historyPath,
        String activityLogPath,
        List<RecentHistory> recentHistories,
        List<Comment> comments
) {
    public static AdminOperationTaskDetailResponse from(
            AdminOperationTask task,
            String assigneeAdminName,
            List<AdminLogListResponse.Item> recentHistories,
            List<AdminOperationTaskCommentResDto> comments
    ) {
        AdminOperationTaskStatus resolvedStatus = AdminOperationTaskStatus.fromCode(task.getStatus());
        AdminOperationTaskPriority resolvedPriority = AdminOperationTaskPriority.fromCode(task.getPriority());
        return new AdminOperationTaskDetailResponse(
                task.getTaskNo(),
                task.getTitle(),
                task.getDescription(),
                resolvedStatus.name(),
                resolvedStatus.getLabel(),
                resolvedPriority.name(),
                resolvedPriority.getLabel(),
                task.getAssigneeAdminNo(),
                assigneeAdminName == null ? "미지정" : assigneeAdminName,
                task.getDueDate() == null ? null : task.getDueDate().toString(),
                resolveDueState(task),
                task.getIsPinned(),
                format(task.getCrtDtm()),
                "/admin/settings/tasks/history?taskNo=" + task.getTaskNo(),
                "/admin/settings/logs?actionType=TASK_&targetId=" + task.getTaskNo(),
                recentHistories == null ? List.of() : recentHistories.stream().map(RecentHistory::from).toList(),
                comments == null ? List.of() : comments.stream().map(Comment::from).toList()
        );
    }

    public record RecentHistory(
            Long logNo,
            String actionType,
            String actionLabel,
            String adminName,
            String actionDtm,
            String activityLogPath,
            String historyPath
    ) {
        public static RecentHistory from(AdminLogListResponse.Item item) {
            return new RecentHistory(
                    item.logNo(),
                    item.actionType(),
                    resolveActionLabel(item.actionType()),
                    item.adminName(),
                    item.actionDtm(),
                    "/admin/settings/logs?actionType=" + item.actionType() + "&targetId=" + item.targetId(),
                    "/admin/settings/tasks/history?taskNo=" + item.targetId()
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

    public record Comment(
            Long commentNo,
            Long adminNo,
            String adminName,
            String content,
            String crtDtm
    ) {
        public static Comment from(AdminOperationTaskCommentResDto dto) {
            return new Comment(
                    dto.getCommentNo(),
                    dto.getAdminNo(),
                    dto.getAdminName() == null ? "관리자#" + dto.getAdminNo() : dto.getAdminName(),
                    dto.getContent(),
                    format(dto.getCrtDtm())
            );
        }
    }

    private static String resolveDueState(AdminOperationTask task) {
        if (task.getDueDate() == null) {
            return "기한 없음";
        }
        if ("DONE".equalsIgnoreCase(task.getStatus())) {
            return "완료";
        }
        LocalDate today = LocalDate.now();
        if (task.getDueDate().isBefore(today)) {
            return "기한 초과";
        }
        if (task.getDueDate().isEqual(today)) {
            return "오늘 마감";
        }
        return "진행중";
    }

    private static String format(LocalDateTime value) {
        return value == null ? "-" : value.toString().replace('T', ' ');
    }
}
