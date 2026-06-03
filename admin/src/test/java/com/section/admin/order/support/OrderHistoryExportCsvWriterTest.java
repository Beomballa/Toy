package com.section.admin.order.support;

import com.section.common.commerce.dto.OrderHistoryListResDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderHistoryExportCsvWriterTest {

    @Test
    @DisplayName("주문 이력 CSV 내보내기는 화면용 라벨과 포맷을 유지한다")
    void writeProducesCsvWithFormattedValues() {
        OrderHistoryListResDto dto = new OrderHistoryListResDto();
        dto.setHistoryNo(11L);
        dto.setOrderNo(7L);
        dto.setActionType("DELIVERY_START");
        dto.setBeforeStatus("PAID");
        dto.setAfterStatus("SHIPPED");
        dto.setReason("출고 시작");
        dto.setAdminMemoSnapshot("문 앞 배송 요청");
        dto.setDeliveryCompany("CJ대한통운");
        dto.setTrackingNum("1234-5678");
        dto.setActorNo(3L);
        dto.setActorName("운영자");
        dto.setActionDtm(LocalDateTime.of(2026, 6, 3, 9, 30));

        byte[] result = OrderHistoryExportCsvWriter.write(List.of(dto));
        String csv = new String(result, StandardCharsets.UTF_8);

        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, new byte[]{result[0], result[1], result[2]});
        assertTrue(csv.contains("이력번호,주문번호,작업유형,이전상태,변경상태,사유,관리메모,택배사,운송장번호,작업자번호,작업자명,작업일시"));
        assertTrue(csv.contains("\"11\",\"7\",\"배송 시작\",\"결제완료\",\"배송중\",\"출고 시작\",\"문 앞 배송 요청\",\"CJ대한통운\",\"1234-5678\",\"3\",\"운영자\",\"2026.06.03 09:30\"\r\n"));
    }
}
