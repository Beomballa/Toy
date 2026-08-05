package com.section.front.memberactivity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FrontMemberActivityReplaceRequest(
        @NotNull @Size(max = 100) List<Long> productIds
) {
}
