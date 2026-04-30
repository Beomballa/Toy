package com.section.admin.order.support;

import com.section.common.commerce.dto.OrderListItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderExportCsvWriterTest {

    @Test
    @DisplayName("주문 CSV 내보내기는 화면 기준 컬럼과 포맷을 유지한다")
    void writeProducesCsvWithFormattedValues() {
        OrderListItemDto dto = new OrderListItemDto();
        dto.setOrderNum("ORD-1");
        dto.setBuyerName("함장님");
        dto.setBuyerPhone("010-1111-2222");
        dto.setFirstProductName("삼바");
        dto.setItemCount(2L);
        dto.setTotalAmount(348000);
        dto.setStatus("PAID");
        dto.setCrtDtm(LocalDateTime.of(2026, 4, 30, 12, 30));

        byte[] result = OrderExportCsvWriter.write(List.of(dto));
        String csv = new String(result, StandardCharsets.UTF_8);

        assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, new byte[] {result[0], result[1], result[2]});
        assertTrue(csv.contains("주문번호,주문일시,주문자명,주문자연락처,상품요약,결제금액,주문상태"));
        assertTrue(csv.contains("\"ORD-1\",\"2026.04.30 12:30\",\"함장님\",\"010-1111-2222\",\"삼바 외 1건\",\"348,000원\",\"결제완료\"\r\n"));
    }
}
