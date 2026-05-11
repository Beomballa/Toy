package com.section.admin.banner.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BannerSaveRequest(
        Long bannerNo,
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 500) String imageUrl,
        @Size(max = 500) String targetUrl,
        @NotNull LocalDateTime startDtm,
        @NotNull LocalDateTime endDtm,
        @NotNull @Min(0) @Max(9999) Integer sortOrder,
        @NotBlank String isActive
) {
}
