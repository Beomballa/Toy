package com.section.admin.task.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AdminOperationTaskSaveRequest(
        Long taskNo,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @NotBlank String status,
        @NotBlank String priority,
        Long assigneeAdminNo,
        LocalDate dueDate,
        String isPinned
) {
}
