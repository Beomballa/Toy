package com.section.admin.settings.support;

import com.section.admin.settings.res.AdminSystemSettingHistoryListResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class AdminSystemSettingHistoryExportCsvWriter {
    private static final String HEADER = "이력번호,변경시각,설정키,설정명,변경전(raw),변경후(raw),변경전(표시값),변경후(표시값),변경요약,관리자번호,관리자명,IP주소";
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private AdminSystemSettingHistoryExportCsvWriter() {
    }

    public static byte[] write(
            AdminSystemSettingHistoryExportSummary summary,
            List<AdminSystemSettingHistoryListResponse.Item> items
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (AdminSystemSettingHistoryListResponse.Item item : items) {
            builder.append(csv(String.valueOf(item.historyNo()))).append(',')
                    .append(csv(item.changedAt())).append(',')
                    .append(csv(item.settingKey())).append(',')
                    .append(csv(item.settingName())).append(',')
                    .append(csv(item.beforeValue())).append(',')
                    .append(csv(item.afterValue())).append(',')
                    .append(csv(item.beforeValueLabel())).append(',')
                    .append(csv(item.afterValueLabel())).append(',')
                    .append(csv(item.changeSummary())).append(',')
                    .append(csv(item.changedAdminNo() == null ? "" : String.valueOf(item.changedAdminNo()))).append(',')
                    .append(csv(item.changedAdminName())).append(',')
                    .append(csv(item.changedIpAddress()))
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
