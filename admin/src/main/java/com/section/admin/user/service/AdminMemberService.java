package com.section.admin.user.service;

import com.section.admin.user.req.AdminMemberListRequest;
import com.section.admin.user.req.AdminMemberBulkStatusUpdateRequest;
import com.section.admin.user.req.AdminMemberStatusUpdateRequest;
import com.section.admin.user.res.AdminMemberDetailResponse;
import com.section.admin.user.res.AdminMemberListResponse;
import com.section.admin.user.res.AdminMemberSummaryResponse;
import com.section.admin.user.support.AdminMemberExportCsvWriter;
import com.section.admin.user.support.AdminMemberExportSummary;
import com.section.common.base.entity.type.YN;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AccountListQuery;
import com.section.common.system.dto.AccountListResDto;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {
    private static final int MEMBER_EXPORT_MAX_SIZE = 1000;

    private final AccountRepository accountRepository;

    public AdminMemberListResponse getMemberList(AdminMemberListRequest req, Pageable pageable) {
        AccountListQuery query = req.toQuery();
        Page<AccountListResDto> page = accountRepository.getAccountList(query, pageable);
        return AdminMemberListResponse.of(page, query);
    }

    public AdminMemberSummaryResponse getMemberSummary(AdminMemberListRequest req) {
        return AdminMemberSummaryResponse.from(accountRepository.getAccountSummary(req.toQuery()));
    }

    public byte[] exportMemberListCsv(AdminMemberListRequest req) {
        AccountListQuery query = req.toQuery();
        Page<AccountListResDto> page = accountRepository.getAccountList(query, PageRequest.of(0, MEMBER_EXPORT_MAX_SIZE));
        AdminMemberListResponse response = AdminMemberListResponse.of(page, query);
        return AdminMemberExportCsvWriter.write(
                AdminMemberExportSummary.of(query, java.time.LocalDateTime.now()),
                response.items()
        );
    }

    public AdminMemberDetailResponse getMemberDetail(Long memberId) {
        return AdminMemberDetailResponse.from(accountRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND)));
    }

    @Transactional
    public void updateMemberStatus(Long memberId, AdminMemberStatusUpdateRequest req) {
        Account account = accountRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.setMasterYn(req.masterMember() ? YN.Y : YN.N);
        account.setDelYn(req.deleted() ? YN.Y : YN.N);
    }

    @Transactional
    public BulkStatusUpdateResult updateMemberStatuses(AdminMemberBulkStatusUpdateRequest req) {
        List<Long> targetMemberIds = req.normalizedMemberIds();
        if (targetMemberIds.isEmpty()) {
            throw new BusinessException("변경할 회원을 선택하세요.", ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!req.hasChanges()) {
            throw new BusinessException("변경할 항목을 하나 이상 선택하세요.", ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Account> accounts = accountRepository.findAllById(targetMemberIds);
        if (accounts.isEmpty()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        int updatedCount = 0;
        int unchangedCount = 0;

        for (Account account : accounts) {
            boolean changed = false;

            if (req.masterMember() != null) {
                YN nextMasterYn = req.masterMember() ? YN.Y : YN.N;
                if (account.getMasterYn() != nextMasterYn) {
                    account.setMasterYn(nextMasterYn);
                    changed = true;
                }
            }

            if (req.deleted() != null) {
                YN nextDelYn = req.deleted() ? YN.Y : YN.N;
                if (account.getDelYn() != nextDelYn) {
                    account.setDelYn(nextDelYn);
                    changed = true;
                }
            }

            if (changed) {
                updatedCount += 1;
            } else {
                unchangedCount += 1;
            }
        }

        Set<Long> existingIds = accounts.stream()
                .map(Account::getId)
                .collect(Collectors.toSet());
        long missingCount = targetMemberIds.stream()
                .filter(id -> !existingIds.contains(id))
                .count();

        return new BulkStatusUpdateResult(targetMemberIds.size(), updatedCount, unchangedCount, (int) missingCount);
    }

    public record BulkStatusUpdateResult(
            int requestedCount,
            int updatedCount,
            int unchangedCount,
            int missingCount
    ) {
    }
}
