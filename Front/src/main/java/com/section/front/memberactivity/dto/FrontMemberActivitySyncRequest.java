package com.section.front.memberactivity.dto;

import com.section.common.commerce.entity.FrontMemberActivityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record FrontMemberActivitySyncRequest(
        @NotNull @Size(max = 4) Map<FrontMemberActivityType, List<Long>> activities
) {
}
