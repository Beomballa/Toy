package com.section.admin.content.support;

import com.section.admin.content.res.ContentPerformanceAnalyticsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContentPerformanceAnalyticsCsvWriterTest {

    @Test
    @DisplayName("콘텐츠 효과 CSV는 BOM과 전체 요약, 우선순위 판단 근거를 포함한다")
    void writesPerformanceAnalytics() {
        ContentPerformanceAnalyticsResponse response = new ContentPerformanceAnalyticsResponse(
                "NOTICE", 7, "2026-07-18", "2026-07-24", "2026-07-24 12:00:00",
                new ContentPerformanceAnalyticsResponse.Summary(100, 5, 40, 5, 3, 2, 1, 1, 1, 1, 1),
                List.of(new ContentPerformanceAnalyticsResponse.Content(
                        1, "NOTICE", "배송, 안내", 50, 20,
                        4, 1, 3, 25, 8, 84,
                        "IMPROVEMENT_REQUIRED", "본문 보완이 필요합니다.",
                        91L, "/admin/settings/tasks?taskNo=91",
                        "IN_PROGRESS", "진행중", "2026-07-23", true, false
                ))
        );

        byte[] bytes = ContentPerformanceAnalyticsCsvWriter.write(response);
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        assertThat(bytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(csv)
                .contains("반응확보율")
                .contains("우선순위점수")
                .contains("미연결조치")
                .contains("연체작업")
                .contains("연결작업상태")
                .contains("IN_PROGRESS")
                .contains("IMPROVEMENT_REQUIRED")
                .contains("91")
                .contains("\"배송, 안내\"");
    }
}
