package com.section.admin.brand.req;

import jakarta.validation.constraints.NotBlank;

public record BrandStatusUpdateRequest(
        @NotBlank String isActive
) {
}
