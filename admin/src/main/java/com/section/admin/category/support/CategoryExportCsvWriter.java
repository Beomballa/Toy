package com.section.admin.category.support;

import com.section.admin.category.res.CategoryResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CategoryExportCsvWriter {
    private static final String HEADER = "카테고리번호,부모카테고리번호,카테고리명,뎁스,상태";
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private CategoryExportCsvWriter() {
    }

    public static byte[] write(CategoryExportSummary summary, List<CategoryResponse> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (CategoryResponse item : items) {
            builder.append(csv(String.valueOf(item.categoryNo()))).append(',')
                    .append(csv(item.parentNo() == null ? "-" : String.valueOf(item.parentNo()))).append(',')
                    .append(csv(item.name())).append(',')
                    .append(csv(item.depth() == null ? "-" : String.valueOf(item.depth()))).append(',')
                    .append(csv("Y".equalsIgnoreCase(item.isActive()) ? "사용중" : "중지"))
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
