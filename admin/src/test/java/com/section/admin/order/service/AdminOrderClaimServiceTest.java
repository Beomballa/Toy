package com.section.admin.order.service;

import com.section.common.base.exception.BusinessException;
import com.section.common.commerce.entity.FrontOrderClaim;
import com.section.common.commerce.entity.FrontOrderClaimHistory;
import com.section.common.commerce.entity.FrontOrderClaimStatus;
import com.section.common.commerce.entity.FrontOrderClaimType;
import com.section.common.commerce.repository.FrontOrderClaimHistoryRepository;
import com.section.common.commerce.repository.FrontOrderClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
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
}
