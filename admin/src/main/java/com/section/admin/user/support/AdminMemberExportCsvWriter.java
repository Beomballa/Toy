package com.section.admin.user.support;

import com.section.admin.user.res.AdminMemberListResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class AdminMemberExportCsvWriter {
    private static final String HEADER = "회원번호,이름,닉네임,이메일,권한,초기화여부,상태,가입일시";
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private AdminMemberExportCsvWriter() {
    }

    public static byte[] write(AdminMemberExportSummary summary, List<AdminMemberListResponse.Item> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (AdminMemberListResponse.Item item : items) {
            builder.append(csv(String.valueOf(item.id()))).append(',')
                    .append(csv(item.name())).append(',')
                    .append(csv(item.nickname())).append(',')
                    .append(csv(item.email())).append(',')
                    .append(csv(resolveMasterYn(item.masterYn()))).append(',')
                    .append(csv(resolveInitYn(item.initYn()))).append(',')
                    .append(csv(resolveDelYn(item.delYn()))).append(',')
                    .append(csv(item.crtDtm()))
                    .append("\r\n");
        }

        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(UTF8_BOM.length + body.length);
        outputStream.writeBytes(UTF8_BOM);
        outputStream.writeBytes(body);
        return outputStream.toByteArray();
    }

    private static String resolveMasterYn(String value) {
        return "Y".equalsIgnoreCase(value) ? "마스터" : "일반회원";
    }

    private static String resolveInitYn(String value) {
        return "Y".equalsIgnoreCase(value) ? "초기화" : "정상";
    }

    private static String resolveDelYn(String value) {
        return "Y".equalsIgnoreCase(value) ? "탈퇴" : "정상";
    }

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
