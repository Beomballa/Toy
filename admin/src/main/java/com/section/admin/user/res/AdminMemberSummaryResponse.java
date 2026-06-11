package com.section.admin.user.res;

import com.section.common.system.dto.AccountSummaryDto;

public record AdminMemberSummaryResponse(
        long totalCount,
        long masterCount,
        long normalCount,
        long deletedCount,
        long tempPasswordCount
) {
    public static AdminMemberSummaryResponse from(AccountSummaryDto dto) {
        return new AdminMemberSummaryResponse(
                dto.totalCount(),
                dto.masterCount(),
                dto.normalCount(),
                dto.deletedCount(),
                dto.tempPasswordCount()
        );
    }
}
