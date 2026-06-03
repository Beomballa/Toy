package com.section.admin.notice.support;

import com.section.common.base.entity.type.AdminNoticeVisibilityStatus;
import com.section.common.system.dto.AdminOperationNoticeListResDto;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class AdminOperationNoticeExportCsvWriter {
    private static final String HEADER = "공지번호,제목,내용요약,활성여부,고정여부,노출상태,노출시작,노출종료,등록일시";
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private AdminOperationNoticeExportCsvWriter() {
    }

    public static byte[] write(AdminOperationNoticeExportSummary summary,
                               List<AdminOperationNoticeListResDto> items,
                               LocalDateTime now) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("정렬")).append(',').append(csv(summary.sortLabel())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (AdminOperationNoticeListResDto item : items) {
            builder.append(csv(String.valueOf(item.getNoticeNo()))).append(',')
                    .append(csv(item.getTitle())).append(',')
                    .append(csv(summarize(item.getContent()))).append(',')
                    .append(csv("Y".equalsIgnoreCase(item.getIsActive()) ? "활성" : "비활성")).append(',')
                    .append(csv("Y".equalsIgnoreCase(item.getIsPinned()) ? "고정" : "일반")).append(',')
                    .append(csv(resolveVisibility(item, now))).append(',')
                    .append(csv(format(item.getStartDtm()))).append(',')
                    .append(csv(format(item.getEndDtm()))).append(',')
                    .append(csv(format(item.getCrtDtm())))
                    .append("\r\n");
        }

        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(UTF8_BOM.length + body.length);
        outputStream.writeBytes(UTF8_BOM);
        outputStream.writeBytes(body);
        return outputStream.toByteArray();
    }

    private static String resolveVisibility(AdminOperationNoticeListResDto item, LocalDateTime now) {
        if (!"Y".equalsIgnoreCase(item.getIsActive())) {
            return AdminNoticeVisibilityStatus.INACTIVE.label();
        }
        if (item.getStartDtm() != null && item.getStartDtm().isAfter(now)) {
            return AdminNoticeVisibilityStatus.SCHEDULED.label();
        }
        if (item.getEndDtm() != null && item.getEndDtm().isBefore(now)) {
            return AdminNoticeVisibilityStatus.ENDED.label();
        }
        return AdminNoticeVisibilityStatus.LIVE.label();
    }

    private static String summarize(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 80) + "...";
    }

    private static String format(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
