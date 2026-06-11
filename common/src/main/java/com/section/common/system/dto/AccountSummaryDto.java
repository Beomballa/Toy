package com.section.common.system.dto;

public record AccountSummaryDto(
        long totalCount,
        long masterCount,
        long normalCount,
        long deletedCount,
        long tempPasswordCount
) {
}
