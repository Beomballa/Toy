package com.section.admin.order.support;

import com.section.common.commerce.dto.OrderListItemDto;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class OrderExportCsvWriter {
    private static final String HEADER = "주문번호,주문일시,주문자명,주문자연락처,상품요약,결제금액,주문상태";
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private OrderExportCsvWriter() {
    }

    public static byte[] write(List<OrderListItemDto> orders) {
        StringBuilder builder = new StringBuilder(HEADER).append("\r\n");

        for (OrderListItemDto order : orders) {
            builder.append(csv(order.getOrderNum())).append(',')
                    .append(csv(OrderViewFormatter.formatDateTime(order.getCrtDtm()))).append(',')
                    .append(csv(order.getBuyerName())).append(',')
                    .append(csv(order.getBuyerPhone())).append(',')
                    .append(csv(OrderViewFormatter.buildProductSummary(order.getFirstProductName(), order.getItemCount()))).append(',')
                    .append(csv(OrderViewFormatter.formatAmount(order.getTotalAmount()))).append(',')
                    .append(csv(OrderViewFormatter.formatStatusDesc(order.getStatus())))
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
        // CSV 셀 내 쉼표/개행/따옴표를 그대로 보존하려면 RFC4180 방식으로 감쌉니다.
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
