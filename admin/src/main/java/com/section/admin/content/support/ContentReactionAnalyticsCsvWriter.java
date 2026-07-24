package com.section.admin.content.support;

import com.section.admin.content.res.ContentReactionAnalyticsResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ContentReactionAnalyticsCsvWriter {

    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private ContentReactionAnalyticsCsvWriter() {
    }

    public static byte[] write(ContentReactionAnalyticsResponse analytics) {
        StringBuilder builder = new StringBuilder();
        appendMetadata(builder, analytics);
        appendTrend(builder, analytics);
        appendContents(builder, "반응 상위 콘텐츠", analytics.topContents());
        appendContents(builder, "개선 필요 콘텐츠", analytics.improvementContents());

        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(UTF8_BOM.length + body.length);
        outputStream.writeBytes(UTF8_BOM);
        outputStream.writeBytes(body);
        return outputStream.toByteArray();
    }

    private static void appendMetadata(StringBuilder builder, ContentReactionAnalyticsResponse analytics) {
        ContentReactionAnalyticsResponse.Summary summary = analytics.summary();
        row(builder, "게시판", analytics.boardType());
        row(builder, "분석기간", analytics.startDate() + " ~ " + analytics.endDate());
        row(builder, "집계기준", analytics.metricBasis());
        row(builder, "생성시각", analytics.generatedAt());
        row(builder, "전체반응", String.valueOf(summary.totalCount()));
        row(builder, "도움됨", String.valueOf(summary.helpfulCount()));
        row(builder, "개선필요", String.valueOf(summary.notHelpfulCount()));
        row(builder, "도움비율", summary.helpfulRate() + "%");
        row(builder, "참여방문자", String.valueOf(summary.uniqueVisitors()));
        row(builder, "평가콘텐츠", String.valueOf(summary.evaluatedContentCount()));
        builder.append("\r\n");
    }

    private static void appendTrend(StringBuilder builder, ContentReactionAnalyticsResponse analytics) {
        builder.append("일자,전체반응,도움됨,개선필요,도움비율\r\n");
        analytics.trend().forEach(item -> builder
                .append(csv(item.date())).append(',')
                .append(csv(String.valueOf(item.totalCount()))).append(',')
                .append(csv(String.valueOf(item.helpfulCount()))).append(',')
                .append(csv(String.valueOf(item.notHelpfulCount()))).append(',')
                .append(csv(item.helpfulRate() + "%"))
                .append("\r\n"));
        builder.append("\r\n");
    }

    private static void appendContents(
            StringBuilder builder,
            String section,
            List<ContentReactionAnalyticsResponse.Content> items
    ) {
        row(builder, "구분", section);
        builder.append("순위,콘텐츠번호,게시판,제목,전체반응,도움됨,개선필요,도움비율\r\n");
        for (int index = 0; index < items.size(); index++) {
            ContentReactionAnalyticsResponse.Content item = items.get(index);
            builder.append(csv(String.valueOf(index + 1))).append(',')
                    .append(csv(String.valueOf(item.documentId()))).append(',')
                    .append(csv(item.boardType())).append(',')
                    .append(csv(item.title())).append(',')
                    .append(csv(String.valueOf(item.totalCount()))).append(',')
                    .append(csv(String.valueOf(item.helpfulCount()))).append(',')
                    .append(csv(String.valueOf(item.notHelpfulCount()))).append(',')
                    .append(csv(item.helpfulRate() + "%"))
                    .append("\r\n");
        }
        builder.append("\r\n");
    }

    private static void row(StringBuilder builder, String label, String value) {
        builder.append(csv(label)).append(',').append(csv(value)).append("\r\n");
    }

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
