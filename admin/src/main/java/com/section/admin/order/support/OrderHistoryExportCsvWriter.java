package com.section.admin.order.support;

import com.section.common.commerce.dto.OrderHistoryListResDto;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class OrderHistoryExportCsvWriter {
    private static final String HEADER = "이력번호,주문번호,작업유형,이전상태,변경상태,사유,관리메모,택배사,운송장번호,작업자번호,작업자명,작업일시";
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private OrderHistoryExportCsvWriter() {
    }

    public static byte[] write(List<OrderHistoryListResDto> histories) {
        StringBuilder builder = new StringBuilder(HEADER).append("\r\n");

        for (OrderHistoryListResDto history : histories) {
            builder.append(csv(history.getHistoryNo() == null ? "" : String.valueOf(history.getHistoryNo()))).append(',')
                    .append(csv(history.getOrderNo() == null ? "" : String.valueOf(history.getOrderNo()))).append(',')
                    .append(csv(OrderViewFormatter.formatActionLabel(history.getActionType()))).append(',')
                    .append(csv(OrderViewFormatter.formatStatusDesc(history.getBeforeStatus()))).append(',')
                    .append(csv(OrderViewFormatter.formatStatusDesc(history.getAfterStatus()))).append(',')
                    .append(csv(history.getReason())).append(',')
                    .append(csv(history.getAdminMemoSnapshot())).append(',')
                    .append(csv(history.getDeliveryCompany())).append(',')
                    .append(csv(history.getTrackingNum())).append(',')
                    .append(csv(history.getActorNo() == null ? "" : String.valueOf(history.getActorNo()))).append(',')
                    .append(csv(resolveActorName(history))).append(',')
                    .append(csv(OrderViewFormatter.formatDateTime(history.getActionDtm())))
                    .append("\r\n");
        }

        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(UTF8_BOM.length + body.length);
        outputStream.writeBytes(UTF8_BOM);
        outputStream.writeBytes(body);
        return outputStream.toByteArray();
    }

    private static String resolveActorName(OrderHistoryListResDto history) {
        if (history.getActorName() != null && !history.getActorName().isBlank()) {
            return history.getActorName();
        }
        return history.getActorNo() == null ? "" : "관리자#" + history.getActorNo();
    }

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
