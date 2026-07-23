package com.section.common.content.repository;

import com.section.common.content.dto.ContentViewDataQualityRow;
import com.section.common.content.dto.ContentViewSummaryRow;
import com.section.common.content.dto.ContentViewTopRow;
import com.section.common.content.dto.ContentViewTrendRow;
import com.section.common.content.entity.Document;

import java.time.LocalDate;
import java.util.List;

public interface CustomFrontContentViewEventRepository {

    ContentViewDataQualityRow getDataQuality();

    ContentViewSummaryRow getViewSummary(LocalDate startDate, LocalDate endDate, Document.BoardType boardType);

    List<ContentViewTrendRow> getDailyViewTrend(LocalDate startDate, LocalDate endDate, Document.BoardType boardType);

    List<ContentViewTopRow> getTopViewedContents(
            LocalDate startDate,
            LocalDate endDate,
            Document.BoardType boardType,
            int limit
    );
}
