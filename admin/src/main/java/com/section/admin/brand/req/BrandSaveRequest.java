package com.section.admin.brand.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandSaveRequest(
        Long brandNo,

        @NotBlank(message = "브랜드명(한글)은 필수입니다.")
        @Size(max = 100, message = "브랜드명(한글)은 100자 이하여야 합니다.")
        String nameKo,

        @Size(max = 100, message = "브랜드명(영문)은 100자 이하여야 합니다.")
        String nameEn,

        @Size(max = 500, message = "로고 URL은 500자 이하여야 합니다.")
        String logoUrl,

        @Size(min = 1, max = 1, message = "사용 여부는 Y 또는 N 이어야 합니다.")
        String isActive
) {}
