package com.section.admin.order.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.base.entity.type.OrderHistoryOrderType;
import com.section.common.commerce.dto.OrderHistoryListQuery;
import com.section.common.commerce.dto.OrderHistoryListResDto;
import com.section.common.commerce.entity.OrderStatusHistory;
import com.section.common.commerce.repository.OrderStatusHistoryRepository;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class OrderStatusHistoryRepositorySearchIntegrationTest {

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Test
    @DisplayName("주문 이력 검색은 공백 단위 키워드와 운송장번호 숫자 검색을 함께 지원한다")
    void getOrderHistoryListSupportsTokenizedKeywordAndTrackingDigits() {
        AdminUser actor = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-history-1")
                .password("pw")
                .name("정산 운영자")
                .build());

        OrderStatusHistory matchedHistory = OrderStatusHistory.create(
                301L,
                "DELIVERY_START",
                "PAID",
                "SHIPPED",
                "새벽 출고 진행",
                "문 앞 전달 요청",
                "CJ대한통운",
                "1234-5678-9000"
        );
        matchedHistory.setCrtNo(actor.getAdminNo());
        matchedHistory.setCrtDtm(LocalDateTime.of(2026, 6, 2, 9, 0));
        orderStatusHistoryRepository.save(matchedHistory);

        OrderStatusHistory otherHistory = OrderStatusHistory.create(
                302L,
                "DELIVERY_START",
                "PAID",
                "SHIPPED",
                "일반 출고",
                "경비실 보관",
                "롯데택배",
                "8888-0000-1111"
        );
        otherHistory.setCrtNo(actor.getAdminNo());
        otherHistory.setCrtDtm(LocalDateTime.of(2026, 6, 2, 10, 0));
        orderStatusHistoryRepository.save(otherHistory);

        Page<OrderHistoryListResDto> result = orderStatusHistoryRepository.getOrderHistoryList(
                new OrderHistoryListQuery(
                        null,
                        null,
                        "출고 문 앞 123456789000",
                        null,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 3),
                        OrderHistoryOrderType.LATEST
                ),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(301L, result.getContent().getFirst().getOrderNo());
    }

    @Test
    @DisplayName("주문 이력 검색은 작업자명을 다중 키워드 AND 조건으로 조회한다")
    void getOrderHistoryListSupportsTokenizedActorKeyword() {
        AdminUser matchedActor = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-history-2")
                .password("pw")
                .name("김 운영 매니저")
                .build());
        AdminUser otherActor = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-history-3")
                .password("pw")
                .name("박 물류 담당")
                .build());

        OrderStatusHistory matchedHistory = OrderStatusHistory.create(
                401L,
                "ADMIN_MEMO",
                "PAID",
                "PAID",
                "메모 저장",
                "VIP 고객",
                null,
                null
        );
        matchedHistory.setCrtNo(matchedActor.getAdminNo());
        matchedHistory.setCrtDtm(LocalDateTime.of(2026, 6, 3, 9, 0));
        orderStatusHistoryRepository.save(matchedHistory);

        OrderStatusHistory otherHistory = OrderStatusHistory.create(
                402L,
                "ADMIN_MEMO",
                "PAID",
                "PAID",
                "메모 저장",
                "일반 고객",
                null,
                null
        );
        otherHistory.setCrtNo(otherActor.getAdminNo());
        otherHistory.setCrtDtm(LocalDateTime.of(2026, 6, 3, 10, 0));
        orderStatusHistoryRepository.save(otherHistory);

        Page<OrderHistoryListResDto> result = orderStatusHistoryRepository.getOrderHistoryList(
                new OrderHistoryListQuery(
                        null,
                        null,
                        null,
                        "운영 매니저",
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 3),
                        OrderHistoryOrderType.LATEST
                ),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(401L, result.getContent().getFirst().getOrderNo());
        assertEquals("김 운영 매니저", result.getContent().getFirst().getActorName());
    }
}
