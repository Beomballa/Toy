package com.section.common.content.dto;

import java.time.LocalDate;

public record ContentViewTrendRow(
        LocalDate viewedDate,
        long viewCount,
        long uniqueVisitors
) {
}
