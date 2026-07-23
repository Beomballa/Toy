package com.section.admin.content.support;

import com.section.admin.content.res.ContentViewAnalyticsResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class ContentViewAnalyticsCsvWriter {

    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private ContentViewAnalyticsCsvWriter() {
    }

    public static byte[] write(ContentViewAnalyticsResponse analytics) {
        StringBuilder builder = new StringBuilder();
        appendMetadata(builder, analytics);
        appendTrend(builder, analytics);
        appendTopContents(builder, analytics);

        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(UTF8_BOM.length + body.length);
        outputStream.writeBytes(UTF8_BOM);
        outputStream.writeBytes(body);
        return outputStream.toByteArray();
    }

    private static void appendMetadata(StringBuilder builder, ContentViewAnalyticsResponse analytics) {
        ContentViewAnalyticsResponse.Summary summary = analytics.summary();
        row(builder, "게시판", analytics.boardType());
        row(builder, "분석기간", analytics.startDate() + " ~ " + analytics.endDate());
        row(builder, "기간일수", analytics.rangeDays() + "일");
        row(builder, "생성시각", analytics.generatedAt());
        row(builder, "기간조회", String.valueOf(summary.totalViews()));
        row(builder, "순방문자", String.valueOf(summary.uniqueVisitors()));
        row(builder, "조회콘텐츠", String.valueOf(summary.viewedContentCount()));
        row(builder, "콘텐츠당평균", String.valueOf(summary.averageViewsPerContent()));
        row(builder, "직전기간조회", String.valueOf(summary.previousViews()));
        row(builder, "조회증감률", summary.viewChangeRate() + "%");
        builder.append("\r\n");
    }

    private static void appendTrend(StringBuilder builder, ContentViewAnalyticsResponse analytics) {
        builder.append("일자,조회수,순방문자\r\n");
        analytics.trend().forEach(item -> builder
                .append(csv(item.date())).append(',')
                .append(csv(String.valueOf(item.viewCount()))).append(',')
                .append(csv(String.valueOf(item.uniqueVisitors())))
                .append("\r\n"));
        builder.append("\r\n");
    }

    private static void appendTopContents(StringBuilder builder, ContentViewAnalyticsResponse analytics) {
        builder.append("순위,콘텐츠번호,게시판,제목,조회수,순방문자\r\n");
        for (int index = 0; index < analytics.topContents().size(); index++) {
            ContentViewAnalyticsResponse.TopContent item = analytics.topContents().get(index);
            builder.append(csv(String.valueOf(index + 1))).append(',')
                    .append(csv(String.valueOf(item.documentId()))).append(',')
                    .append(csv(item.boardType())).append(',')
                    .append(csv(item.title())).append(',')
                    .append(csv(String.valueOf(item.viewCount()))).append(',')
                    .append(csv(String.valueOf(item.uniqueVisitors())))
                    .append("\r\n");
        }
    }

    private static void row(StringBuilder builder, String label, String value) {
        builder.append(csv(label)).append(',').append(csv(value)).append("\r\n");
    }

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
