package com.section.admin.task.res;

import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.system.entity.AdminOperationTask;

public record AdminOperationTaskDetailResponse(
        Long taskNo,
        String title,
        String description,
        String status,
        String priority,
        Long assigneeAdminNo,
        String dueDate,
        String isPinned
) {
    public static AdminOperationTaskDetailResponse from(AdminOperationTask task) {
        return new AdminOperationTaskDetailResponse(
                task.getTaskNo(),
                task.getTitle(),
                task.getDescription(),
                AdminOperationTaskStatus.fromCode(task.getStatus()).name(),
                AdminOperationTaskPriority.fromCode(task.getPriority()).name(),
                task.getAssigneeAdminNo(),
                task.getDueDate() == null ? null : task.getDueDate().toString(),
                task.getIsPinned()
        );
    }
}
