package com.section.admin.task.req;

import jakarta.validation.constraints.NotBlank;

public record AdminOperationTaskCommentSaveRequest(
        @NotBlank String content
) {
}
