package com.section.admin.product.support;

import com.section.common.base.entity.type.ProductHistoryActionType;
import com.section.common.commerce.dto.ProductHistoryListResDto;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ProductHistoryExportCsvWriter {
    private static final String HEADER = "이력번호,상품번호,작업유형,요약,상태스냅샷,옵션수,총재고,작업자번호,작업자명,작업일시";
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private ProductHistoryExportCsvWriter() {
    }

    public static byte[] write(ProductHistoryExportSummary summary, List<ProductHistoryListResDto> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("정렬")).append(',').append(csv(summary.orderTypeLabel())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (ProductHistoryListResDto item : items) {
            builder.append(csv(stringValue(item.getHistoryNo()))).append(',')
                    .append(csv(stringValue(item.getProductNo()))).append(',')
                    .append(csv(resolveActionLabel(item.getActionType()))).append(',')
                    .append(csv(item.getSummary())).append(',')
                    .append(csv(item.getStatusSnapshot())).append(',')
                    .append(csv(stringValue(item.getOptionCount()))).append(',')
                    .append(csv(formatStock(item.getTotalStock()))).append(',')
                    .append(csv(stringValue(item.getActorNo()))).append(',')
                    .append(csv(resolveActorName(item))).append(',')
                    .append(csv(ProductViewFormatter.formatExportedAt(item.getActionDtm())))
                    .append("\r\n");
        }

        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(UTF8_BOM.length + body.length);
        outputStream.writeBytes(UTF8_BOM);
        outputStream.writeBytes(body);
        return outputStream.toByteArray();
    }

    private static String resolveActionLabel(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return "";
        }
        return switch (ProductHistoryActionType.valueOf(actionType)) {
            case CREATED -> "등록";
            case UPDATED -> "수정";
            case DELETED -> "삭제";
        };
    }

    private static String resolveActorName(ProductHistoryListResDto item) {
        if (item.getActorName() != null && !item.getActorName().isBlank()) {
            return item.getActorName();
        }
        return item.getActorNo() == null ? "" : "관리자#" + item.getActorNo();
    }

    private static String formatStock(Long totalStock) {
        if (totalStock == null) {
            return "0개";
        }
        return String.format("%,d개", totalStock);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
