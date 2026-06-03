package com.section.admin.content.support;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.entity.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentExportSummaryTest {

    @Test
    @DisplayName("콘텐츠 내보내기 요약은 게시판과 상태를 한글 라벨로 만든다")
    void fromBuildsLocalizedFilterSummary() {
        ContentExportSummary summary = ContentExportSummary.from(new DocumentListQuery(
                Document.BoardType.NOTICE,
                "점검",
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                true,
                null,
                null
        ));

        assertEquals("게시판: 공지 | 검색어: 점검 | 상태: 게시중 | 공개여부: 공개 | 고정만", summary.filterSummary());
        assertEquals("고정 우선 · 최신 등록 순", summary.sortLabel());
    }
}
