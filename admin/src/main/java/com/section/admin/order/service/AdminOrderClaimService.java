package com.section.admin.order.service;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.entity.FrontOrderClaim;
import com.section.common.commerce.entity.FrontOrderClaimHistory;
import com.section.common.commerce.entity.FrontOrderClaimStatus;
import com.section.common.commerce.repository.FrontOrderClaimHistoryRepository;
import com.section.common.commerce.repository.FrontOrderClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminOrderClaimService {
    private final FrontOrderClaimRepository claimRepository;
    private final FrontOrderClaimHistoryRepository claimHistoryRepository;

    @Transactional
    public void updateStatus(long claimNo, FrontOrderClaimStatus nextStatus, String memo) {
        FrontOrderClaim claim = claimRepository.findByIdForUpdate(claimNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        FrontOrderClaimStatus beforeStatus = claim.getStatus();
        if (!isAllowedTransition(beforeStatus, nextStatus)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        claim.changeStatus(nextStatus, LocalDateTime.now());
        claimHistoryRepository.save(FrontOrderClaimHistory.create(claim.getId(), beforeStatus, nextStatus, memo));
    }

    private boolean isAllowedTransition(FrontOrderClaimStatus beforeStatus, FrontOrderClaimStatus nextStatus) {
        return (beforeStatus == FrontOrderClaimStatus.REQUESTED
                && (nextStatus == FrontOrderClaimStatus.APPROVED || nextStatus == FrontOrderClaimStatus.REJECTED))
                || (beforeStatus == FrontOrderClaimStatus.APPROVED && nextStatus == FrontOrderClaimStatus.COMPLETED);
    }
}
