package com.section.admin.order.service;

import com.section.common.base.exception.BusinessException;
import com.section.common.commerce.entity.FrontOrderClaim;
import com.section.common.commerce.entity.FrontOrderClaimHistory;
import com.section.common.commerce.entity.FrontOrderClaimStatus;
import com.section.common.commerce.entity.FrontOrderClaimType;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.repository.FrontOrderClaimHistoryRepository;
import com.section.common.commerce.repository.FrontOrderClaimRepository;
import com.section.common.commerce.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AdminOrderClaimServiceTest {
    @Mock private FrontOrderClaimRepository claimRepository;
    @Mock private FrontOrderClaimHistoryRepository claimHistoryRepository;
    @Mock private OrderRepository orderRepository;
    @InjectMocks private AdminOrderClaimService service;

    @Test
    void approvesRequestedClaimAndCreatesHistory() {
        FrontOrderClaim claim = FrontOrderClaim.request(12L, 7L, FrontOrderClaimType.RETURN, "상품 상태 확인", LocalDateTime.now());
        ReflectionTestUtils.setField(claim, "id", 33L);
        when(claimRepository.findByIdForUpdate(33L)).thenReturn(Optional.of(claim));

        service.updateStatus(33L, FrontOrderClaimStatus.APPROVED, "회수 접수 완료");

        assertEquals(FrontOrderClaimStatus.APPROVED, claim.getStatus());
        ArgumentCaptor<FrontOrderClaimHistory> historyCaptor = ArgumentCaptor.forClass(FrontOrderClaimHistory.class);
        verify(claimHistoryRepository).save(historyCaptor.capture());
        assertEquals("REQUESTED", historyCaptor.getValue().getBeforeStatus());
        assertEquals("APPROVED", historyCaptor.getValue().getAfterStatus());
        assertEquals("회수 접수 완료", historyCaptor.getValue().getMemo());
    }

    @Test
    void rejectsCompletingRequestedClaimWithoutApproval() {
        FrontOrderClaim claim = FrontOrderClaim.request(12L, 7L, FrontOrderClaimType.EXCHANGE, "사이즈 교환", LocalDateTime.now());
        ReflectionTestUtils.setField(claim, "id", 33L);
        when(claimRepository.findByIdForUpdate(33L)).thenReturn(Optional.of(claim));

        assertThrows(BusinessException.class, () -> service.updateStatus(33L, FrontOrderClaimStatus.COMPLETED, "처리 완료"));

        assertEquals(FrontOrderClaimStatus.REQUESTED, claim.getStatus());
        verify(claimHistoryRepository, never()).save(any());
    }

    @Test
    void listsClaimsWithBatchMappedOrderNumbers() {
        FrontOrderClaim claim = FrontOrderClaim.request(12L, 7L, FrontOrderClaimType.EXCHANGE, "사이즈 교환", LocalDateTime.now());
        ReflectionTestUtils.setField(claim, "id", 33L);
        Orders order = Orders.createOrder("GS20260907120000001A", "홍길동", "01000000000", 10000, 7L);
        ReflectionTestUtils.setField(order, "id", 12L);
        when(claimRepository.findAllByOrderByIdDesc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(claim), PageRequest.of(0, 20), 1));
        when(orderRepository.findAllById(List.of(12L))).thenReturn(List.of(order));

        var response = service.getClaims("ALL", 0);

        assertEquals(1, response.claims().size());
        assertEquals("GS20260907120000001A", response.claims().get(0).orderNumber());
        assertEquals("교환", response.claims().get(0).claimTypeLabel());
        verify(orderRepository).findAllById(List.of(12L));
    }
}
