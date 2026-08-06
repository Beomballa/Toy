package com.section.front.commerce.dto;

import jakarta.validation.constraints.Size;

public record FrontMemberOrderCancelRequest(
        @Size(max = 200) String reason
) {
}
