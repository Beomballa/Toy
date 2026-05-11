package com.section.admin.user.req;

import jakarta.validation.constraints.NotNull;

public record AdminMemberStatusUpdateRequest(
        @NotNull Boolean masterMember,
        @NotNull Boolean deleted
) {
}
