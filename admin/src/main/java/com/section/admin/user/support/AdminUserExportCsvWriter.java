package com.section.admin.user.support;

import com.section.admin.user.res.AdminUserListResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class AdminUserExportCsvWriter {
    private static final String HEADER = "관리자번호,로그인ID,관리자명,권한,상태,마지막로그인,생성일시";
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private AdminUserExportCsvWriter() {
    }

    public static byte[] write(AdminUserExportSummary summary, List<AdminUserListResponse.Item> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (AdminUserListResponse.Item item : items) {
            builder.append(csv(String.valueOf(item.adminNo()))).append(',')
                    .append(csv(item.loginId())).append(',')
                    .append(csv(item.name())).append(',')
                    .append(csv(item.roleLabel())).append(',')
                    .append(csv(item.statusLabel())).append(',')
                    .append(csv(item.lastLoginDtm())).append(',')
                    .append(csv(item.crtDtm()))
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
