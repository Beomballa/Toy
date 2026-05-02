package com.section.admin.product.support;

import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.ProductListResDto;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ProductExportCsvWriter {
    private static final String HEADER = "상품번호,상품명,모델번호,브랜드,발매가,총재고,상태,등록일시";
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private ProductExportCsvWriter() {
    }

    public static byte[] write(List<ProductListResDto> products) {
        StringBuilder builder = new StringBuilder(HEADER).append("\r\n");

        for (ProductListResDto product : products) {
            builder.append(csv(String.valueOf(product.getProductNo()))).append(',')
                    .append(csv(product.getProductName())).append(',')
                    .append(csv(product.getProductModel())).append(',')
                    .append(csv(product.getBrandName())).append(',')
                    .append(csv(formatPrice(product.getReleasePrice()))).append(',')
                    .append(csv(formatStock(product.getTotalStock()))).append(',')
                    .append(csv(ProductStatus.fromCode(product.getStatus()).getDesc())).append(',')
                    .append(csv(ProductViewFormatter.formatCreatedAt(product.getCrtDtm())))
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

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
