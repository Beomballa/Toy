package com.section.front.content.dto;

public record FrontContentReactionResponse(
        long helpfulCount,
        long notHelpfulCount,
        long totalCount,
        int helpfulRate,
        String selectedReaction,
        boolean changed
) {
}
