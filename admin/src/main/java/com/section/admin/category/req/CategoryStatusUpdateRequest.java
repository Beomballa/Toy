package com.section.admin.category.req;

import jakarta.validation.constraints.NotBlank;

public record CategoryStatusUpdateRequest(
        @NotBlank String isActive
) {
}
