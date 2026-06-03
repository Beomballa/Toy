package com.section.admin.log.support;

import com.section.admin.log.res.AdminLogListResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class AdminLogExportCsvWriter {
    private static final String HEADER = "로그번호,관리자번호,관리자명,작업종류,대상ID,대상라벨,대상이동경로,IP주소,작업일시";
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private AdminLogExportCsvWriter() {
    }

    public static byte[] write(AdminLogExportSummary summary, List<AdminLogListResponse.Item> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (AdminLogListResponse.Item item : items) {
            builder.append(csv(String.valueOf(item.logNo()))).append(',')
                    .append(csv(String.valueOf(item.adminNo()))).append(',')
                    .append(csv(item.adminName())).append(',')
                    .append(csv(item.actionType())).append(',')
                    .append(csv(item.targetId() == null ? "" : String.valueOf(item.targetId()))).append(',')
                    .append(csv(item.targetLabel())).append(',')
                    .append(csv(item.targetPath())).append(',')
                    .append(csv(item.ipAddress())).append(',')
                    .append(csv(item.actionDtm()))
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
