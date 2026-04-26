package com.section.admin.category.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategorySaveRequest(
        Long categoryNo,
        Long parentNo,

        @NotBlank(message = "카테고리명은 필수입니다.")
        @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다.")
        String name,

        @NotNull(message = "카테고리 depth는 필수입니다.")
        Integer depth,

        @Size(min = 1, max = 1, message = "사용 여부는 Y 또는 N 이어야 합니다.")
        String isActive
) {}
