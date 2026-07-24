package com.section.admin.content.support;

import com.section.admin.content.res.ContentReactionAnalyticsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContentReactionAnalyticsCsvWriterTest {

    @Test
    @DisplayName("반응 분석 CSV는 BOM과 집계 기준, 추이, 개선 콘텐츠를 포함한다")
    void writesReactionAnalyticsSections() {
        ContentReactionAnalyticsResponse analytics = new ContentReactionAnalyticsResponse(
                "NOTICE", 7, "2026-07-18", "2026-07-24", "2026-07-24 12:00:00",
                "기간 내 마지막 선택 시각 기준 현재 반응",
                new ContentReactionAnalyticsResponse.Summary(3, 2, 1, 67, 3, 1),
                List.of(new ContentReactionAnalyticsResponse.Trend("2026-07-24", 3, 2, 1, 67)),
                List.of(new ContentReactionAnalyticsResponse.Content(1, "NOTICE", "배송, 안내", 3, 2, 1, 67)),
                List.of(new ContentReactionAnalyticsResponse.Content(1, "NOTICE", "배송, 안내", 3, 2, 1, 67))
        );

        byte[] bytes = ContentReactionAnalyticsCsvWriter.write(analytics);
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        assertThat(bytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(csv)
                .contains("집계기준")
                .contains("일자,전체반응,도움됨,개선필요,도움비율")
                .contains("개선 필요 콘텐츠")
                .contains("\"배송, 안내\"");
    }
}
