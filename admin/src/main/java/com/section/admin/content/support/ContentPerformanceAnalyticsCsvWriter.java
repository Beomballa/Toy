package com.section.admin.content.support;

import com.section.admin.content.res.ContentPerformanceAnalyticsResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class ContentPerformanceAnalyticsCsvWriter {

    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private ContentPerformanceAnalyticsCsvWriter() {
    }

    public static byte[] write(ContentPerformanceAnalyticsResponse analytics) {
        StringBuilder builder = new StringBuilder();
        ContentPerformanceAnalyticsResponse.Summary summary = analytics.summary();
        row(builder, "게시판", analytics.boardType());
        row(builder, "분석기간", analytics.startDate() + " ~ " + analytics.endDate());
        row(builder, "생성시각", analytics.generatedAt());
        row(builder, "전체조회", String.valueOf(summary.totalViews()));
        row(builder, "전체반응", String.valueOf(summary.totalReactions()));
        row(builder, "도움비율", summary.helpfulRate() + "%");
        row(builder, "반응확보율", summary.reactionCoverageRate() + "%");
        row(builder, "분석콘텐츠", String.valueOf(summary.analyzedContentCount()));
        row(builder, "조치필요", String.valueOf(summary.actionRequiredCount()));
        row(builder, "작업연결", String.valueOf(summary.linkedActionCount()));
        row(builder, "미연결조치", String.valueOf(summary.unlinkedActionCount()));
        row(builder, "진행작업", String.valueOf(summary.openTaskCount()));
        row(builder, "연체작업", String.valueOf(summary.overdueTaskCount()));
        row(builder, "회복완료후보", String.valueOf(summary.recoverableTaskCount()));
        builder.append("\r\n");
        builder.append("순위,콘텐츠번호,게시판,제목,조회수,순방문자,반응수,도움됨,개선필요,도움비율,반응확보율,우선순위점수,상태,판단근거,연결작업번호,연결작업상태,연결작업기한,연체,회복완료후보,연결작업경로\r\n");
        for (int index = 0; index < analytics.priorityContents().size(); index++) {
            ContentPerformanceAnalyticsResponse.Content item = analytics.priorityContents().get(index);
            builder.append(csv(String.valueOf(index + 1))).append(',')
                    .append(csv(String.valueOf(item.documentId()))).append(',')
                    .append(csv(item.boardType())).append(',')
                    .append(csv(item.title())).append(',')
                    .append(csv(String.valueOf(item.viewCount()))).append(',')
                    .append(csv(String.valueOf(item.uniqueVisitors()))).append(',')
                    .append(csv(String.valueOf(item.reactionCount()))).append(',')
                    .append(csv(String.valueOf(item.helpfulCount()))).append(',')
                    .append(csv(String.valueOf(item.notHelpfulCount()))).append(',')
                    .append(csv(item.helpfulRate() + "%")).append(',')
                    .append(csv(item.reactionCoverageRate() + "%")).append(',')
                    .append(csv(String.valueOf(item.priorityScore()))).append(',')
                    .append(csv(item.status())).append(',')
                    .append(csv(item.statusMessage())).append(',')
                    .append(csv(item.operationTaskNo() == null ? "" : String.valueOf(item.operationTaskNo()))).append(',')
                    .append(csv(item.operationTaskStatus())).append(',')
                    .append(csv(item.operationTaskDueDate())).append(',')
                    .append(csv(item.operationTaskOverdue() ? "Y" : "N")).append(',')
                    .append(csv(item.operationTaskRecoverable() ? "Y" : "N")).append(',')
                    .append(csv(item.operationTaskPath()))
                    .append("\r\n");
        }
        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(UTF8_BOM.length + body.length);
        outputStream.writeBytes(UTF8_BOM);
        outputStream.writeBytes(body);
        return outputStream.toByteArray();
    }

    private static void row(StringBuilder builder, String label, String value) {
        builder.append(csv(label)).append(',').append(csv(value)).append("\r\n");
    }

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
