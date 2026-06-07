package com.section.admin.product.support;

import com.section.admin.product.res.ProductFrontDisplayListResponse;
import com.section.common.base.entity.type.ProductStatus;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ProductFrontDisplayExportCsvWriter {
    private static final String HEADER = "상품번호,상품명,브랜드,카테고리,발매가,총재고,상태,노출설정,Featured,노출순서,헤드라인,무드,설명";
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private ProductFrontDisplayExportCsvWriter() {
    }

    public static byte[] write(
            ProductFrontDisplayExportSummary summary,
            List<ProductFrontDisplayListResponse> items
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("정렬")).append(',').append(csv(summary.sortLabel())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (ProductFrontDisplayListResponse item : items) {
            builder.append(csv(String.valueOf(item.productNo()))).append(',')
                    .append(csv(item.productName())).append(',')
                    .append(csv(item.brandName())).append(',')
                    .append(csv(item.categoryName())).append(',')
                    .append(csv(formatPrice(item.releasePrice()))).append(',')
                    .append(csv(formatStock(item.totalStock()))).append(',')
                    .append(csv(formatStatus(item.status()))).append(',')
                    .append(csv(item.displayConfigured() ? "설정됨" : "미설정")).append(',')
                    .append(csv(item.featured() ? "Y" : "N")).append(',')
                    .append(csv(item.featured() ? String.valueOf(item.featuredRank()) : "-")).append(',')
                    .append(csv(item.headline())).append(',')
                    .append(csv(item.mood())).append(',')
                    .append(csv(item.description()))
                    .append("\r\n");
        }

        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(UTF8_BOM.length + body.length);
        outputStream.writeBytes(UTF8_BOM);
        outputStream.writeBytes(body);
        return outputStream.toByteArray();
    }

    private static String formatPrice(Integer releasePrice) {
        if (releasePrice == null) {
            return "-";
        }
        return String.format("%,d원", releasePrice);
    }

    private static String formatStock(Long totalStock) {
        if (totalStock == null) {
            return "0개";
        }
        return String.format("%,d개", totalStock);
    }

    private static String formatStatus(String status) {
        if (status == null || status.isBlank()) {
            return "-";
        }
        return ProductStatus.fromCode(status).getDesc();
    }

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
