package com.section.front.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FrontMemberProfileUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String nickname
) {
}
