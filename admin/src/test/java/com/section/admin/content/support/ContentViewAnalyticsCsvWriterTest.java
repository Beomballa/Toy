package com.section.admin.content.support;

import com.section.admin.content.res.ContentViewAnalyticsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContentViewAnalyticsCsvWriterTest {

    @Test
    @DisplayName("조회 분석 CSV는 BOM과 요약, 추이, 상위 콘텐츠를 포함한다")
    void writesSummaryTrendAndRankingSections() {
        ContentViewAnalyticsResponse analytics = new ContentViewAnalyticsResponse(
                "NOTICE",
                7,
                "2026-07-17",
                "2026-07-23",
                "2026-07-23 15:00:00",
                new ContentViewAnalyticsResponse.Summary(12, 8, 2, 6.0, 10, 20),
                List.of(new ContentViewAnalyticsResponse.Trend("2026-07-23", 7, 5)),
                List.of(new ContentViewAnalyticsResponse.TopContent(
                        3L, "NOTICE", "배송 \"긴급\" 공지", 7, 5
                ))
        );

        byte[] bytes = ContentViewAnalyticsCsvWriter.write(analytics);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(bytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(csv).contains("\"분석기간\",\"2026-07-17 ~ 2026-07-23\"");
        assertThat(csv).contains("\"조회증감률\",\"20%\"");
        assertThat(csv).contains("일자,조회수,순방문자");
        assertThat(csv).contains("\"2026-07-23\",\"7\",\"5\"");
        assertThat(csv).contains("순위,콘텐츠번호,게시판,제목,조회수,순방문자");
        assertThat(csv).contains("\"배송 \"\"긴급\"\" 공지\"");
    }
}
