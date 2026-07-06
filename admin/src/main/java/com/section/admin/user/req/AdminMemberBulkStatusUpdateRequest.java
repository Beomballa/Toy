package com.section.admin.user.req;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdminMemberBulkStatusUpdateRequest(
        @NotEmpty List<Long> memberIds,
        Boolean masterMember,
        Boolean deleted
) {

    public List<Long> normalizedMemberIds() {
        return memberIds == null ? List.of() : memberIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    public boolean hasChanges() {
        return masterMember != null || deleted != null;
    }
}
