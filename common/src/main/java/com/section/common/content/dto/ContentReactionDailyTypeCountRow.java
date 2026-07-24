package com.section.common.content.dto;

import com.section.common.content.entity.FrontContentReaction;

import java.sql.Date;

public record ContentReactionDailyTypeCountRow(
        Date reactedDate,
        FrontContentReaction.ReactionType reactionType,
        long count
) {
}
