package com.section.common.content.dto;

import java.time.LocalDate;

public record ContentReactionTrendRow(
        LocalDate reactedDate,
        long helpfulCount,
        long notHelpfulCount
) {
    public long totalCount() {
        return helpfulCount + notHelpfulCount;
    }
}
