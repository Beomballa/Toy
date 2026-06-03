package com.section.admin.banner.support;

import com.section.common.commerce.dto.BannerListResDto;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class BannerExportCsvWriter {
    private static final String HEADER = "배너번호,정렬순서,제목,상태,노출시작,노출종료,이동URL,이미지URL";
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private BannerExportCsvWriter() {
    }

    public static byte[] write(BannerExportSummary summary, List<BannerListResDto> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("정렬")).append(',').append(csv(summary.sortLabel())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (BannerListResDto item : items) {
            builder.append(csv(String.valueOf(item.getBannerNo()))).append(',')
                    .append(csv(String.valueOf(item.getSortOrder()))).append(',')
                    .append(csv(item.getTitle())).append(',')
                    .append(csv(resolveDisplayStatus(item))).append(',')
                    .append(csv(format(item.getStartDtm()))).append(',')
                    .append(csv(format(item.getEndDtm()))).append(',')
                    .append(csv(item.getTargetUrl())).append(',')
                    .append(csv(item.getImageUrl()))
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

    private static String format(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }

    private static String resolveDisplayStatus(BannerListResDto item) {
        if (!"Y".equalsIgnoreCase(item.getIsActive())) {
            return "중지";
        }
        LocalDateTime now = LocalDateTime.now();
        if (item.getStartDtm() != null && item.getStartDtm().isAfter(now)) {
            return "대기";
        }
        if (item.getEndDtm() != null && item.getEndDtm().isBefore(now)) {
            return "종료";
        }
        return "노출중";
    }
}
