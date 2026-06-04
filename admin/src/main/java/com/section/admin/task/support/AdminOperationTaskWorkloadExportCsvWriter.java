package com.section.admin.task.support;

import com.section.admin.task.res.AdminOperationTaskWorkloadListResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class AdminOperationTaskWorkloadExportCsvWriter {
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String HEADER = "담당자번호,담당자명,전체작업,대기,진행중,기한초과,최근메모작업,최근메모작성자,최근메모시각,최근메모";

    private AdminOperationTaskWorkloadExportCsvWriter() {
    }

    public static byte[] write(
            AdminOperationTaskWorkloadExportSummary summary,
            List<AdminOperationTaskWorkloadListResponse.Item> items
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸일자")).append(',').append(csv(summary.exportedDate())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (AdminOperationTaskWorkloadListResponse.Item item : items) {
            builder.append(csv(String.valueOf(item.assigneeAdminNo()))).append(',')
                    .append(csv(item.assigneeAdminName())).append(',')
                    .append(csv(String.valueOf(item.totalCount()))).append(',')
                    .append(csv(String.valueOf(item.todoCount()))).append(',')
                    .append(csv(String.valueOf(item.inProgressCount()))).append(',')
                    .append(csv(String.valueOf(item.overdueCount()))).append(',')
                    .append(csv(item.latestCommentTaskTitle())).append(',')
                    .append(csv(item.latestCommentAdminName())).append(',')
                    .append(csv(item.latestCommentDtm())).append(',')
                    .append(csv(item.latestCommentContent()))
                    .append("\r\n");
        }

        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(UTF8_BOM.length + body.length);
        outputStream.writeBytes(UTF8_BOM);
        outputStream.writeBytes(body);
        return outputStream.toByteArray();
    }

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
