package com.section.admin.content.support;

import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.entity.Document;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ContentExportCsvWriter {
    private static final String HEADER = "게시글번호,게시판,제목,상태,공개여부,고정여부,조회수,등록일시,내용미리보기";
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ContentExportCsvWriter() {
    }

    public static byte[] write(ContentExportSummary summary, List<DocumentListItemDto> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("정렬")).append(',').append(csv(summary.sortLabel())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (DocumentListItemDto item : items) {
            builder.append(csv(String.valueOf(item.getId()))).append(',')
                    .append(csv(resolveBoardTypeLabel(item.getBoardType()))).append(',')
                    .append(csv(item.getTitle())).append(',')
                    .append(csv(resolveStatusLabel(item.getStatus()))).append(',')
                    .append(csv("Y".equalsIgnoreCase(item.getPublicYn()) ? "공개" : "비공개")).append(',')
                    .append(csv("Y".equalsIgnoreCase(item.getPinnedYn()) ? "고정" : "일반")).append(',')
                    .append(csv(String.valueOf(item.getViewCnt()))).append(',')
                    .append(csv(item.getCrtDtm() == null ? "-" : item.getCrtDtm().format(DATE_TIME_FORMATTER))).append(',')
                    .append(csv(ContentPreviewSanitizer.sanitize(item.getContentPreview())))
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

    private static String resolveBoardTypeLabel(String boardType) {
        if (boardType == null || boardType.isBlank()) {
            return "-";
        }
        try {
            return switch (Document.BoardType.valueOf(boardType)) {
                case NOTICE -> "공지";
                case STYLE -> "스타일";
                case DISCUSS -> "토론";
                case QNA -> "문의";
            };
        } catch (IllegalArgumentException exception) {
            return boardType;
        }
    }

    private static String resolveStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "-";
        }
        try {
            return switch (Document.PublishStatus.valueOf(status)) {
                case DRAFT -> "임시저장";
                case PUBLISHED -> "게시중";
            };
        } catch (IllegalArgumentException exception) {
            return status;
        }
    }
}
