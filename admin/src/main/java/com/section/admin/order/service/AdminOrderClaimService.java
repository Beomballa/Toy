package com.section.admin.order.service;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.entity.FrontOrderClaim;
import com.section.common.commerce.entity.FrontOrderClaimHistory;
import com.section.common.commerce.entity.FrontOrderClaimStatus;
import com.section.common.commerce.repository.FrontOrderClaimHistoryRepository;
import com.section.common.commerce.repository.FrontOrderClaimRepository;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.section.admin.order.res.AdminOrderClaimListResponse;

@Service
@RequiredArgsConstructor
public class AdminOrderClaimService {
    private final FrontOrderClaimRepository claimRepository;
    private final FrontOrderClaimHistoryRepository claimHistoryRepository;
    private final OrderRepository orderRepository;

    public AdminOrderClaimListResponse getClaims(String rawStatus, int page) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        FrontOrderClaimStatus status = resolveStatus(rawStatus);
        Page<FrontOrderClaim> claims = status == null
                ? claimRepository.findAllByOrderByIdDesc(PageRequest.of(page, 20))
                : claimRepository.findByStatusOrderByIdDesc(status, PageRequest.of(page, 20));
        Map<Long, String> orderNumbers = claims.isEmpty() ? Map.of() : orderRepository.findAllById(
                claims.getContent().stream().map(FrontOrderClaim::getOrderNo).toList()
        ).stream().collect(Collectors.toMap(Orders::getId, Orders::getOrderNum));
        List<AdminOrderClaimListResponse.Item> items = claims.getContent().stream()
                .map(claim -> new AdminOrderClaimListResponse.Item(
                        claim.getId(), claim.getOrderNo(), orderNumbers.getOrDefault(claim.getOrderNo(), "-"), claim.getMemberNo(),
                        claim.getClaimType().name(), claim.getClaimType().name().equals("EXCHANGE") ? "교환" : "반품",
                        claim.getStatus().name(), statusLabel(claim.getStatus()), claim.getReason(),
                        claim.getRequestedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                )).toList();
        return new AdminOrderClaimListResponse(items, claims.getNumber(), claims.getTotalPages(), claims.getTotalElements(), claims.hasNext());
    }

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

    private FrontOrderClaimStatus resolveStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank() || "ALL".equalsIgnoreCase(rawStatus)) {
            return null;
        }
        try {
            return FrontOrderClaimStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String statusLabel(FrontOrderClaimStatus status) {
        return switch (status) {
            case REQUESTED -> "접수 완료";
            case APPROVED -> "처리 진행";
            case REJECTED -> "처리 불가";
            case COMPLETED -> "처리 완료";
        };
    }
}
