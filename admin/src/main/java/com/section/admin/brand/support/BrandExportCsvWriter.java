package com.section.admin.brand.support;

import com.section.admin.brand.res.BrandResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class BrandExportCsvWriter {
    private static final String HEADER = "브랜드번호,브랜드명(한글),브랜드명(영문),상태,로고URL";
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private BrandExportCsvWriter() {
    }

    public static byte[] write(BrandExportSummary summary, List<BrandResponse> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (BrandResponse item : items) {
            builder.append(csv(String.valueOf(item.brandNo()))).append(',')
                    .append(csv(item.nameKo())).append(',')
                    .append(csv(item.nameEn())).append(',')
                    .append(csv("Y".equalsIgnoreCase(item.isActive()) ? "사용중" : "중지")).append(',')
                    .append(csv(item.logoUrl()))
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
