package com.section.common.content.dto;

import com.section.common.content.entity.FrontContentReaction;

public record ContentReactionCountRow(
        FrontContentReaction.ReactionType reactionType,
        long count
) {
}
