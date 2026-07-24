package com.section.common.content.repository;

import com.section.common.content.dto.ContentReactionSummaryRow;

public interface CustomFrontContentReactionRepository {

    ContentReactionSummaryRow getSummary(long documentNo);
}
