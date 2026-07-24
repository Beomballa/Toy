package com.section.common.content.dto;

import com.section.common.content.entity.FrontContentReaction;

public record ContentReactionDocumentTypeCountRow(
        long documentId,
        FrontContentReaction.ReactionType reactionType,
        long count
) {
}
