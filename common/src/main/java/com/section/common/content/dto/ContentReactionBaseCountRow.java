package com.section.common.content.dto;

public record ContentReactionBaseCountRow(
        long totalCount,
        long uniqueVisitors,
        long evaluatedContentCount
) {
}
