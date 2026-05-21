package com.section.admin.notice.service;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.admin.notice.req.AdminOperationNoticeBulkOperateRequest;
import com.section.admin.notice.req.AdminOperationNoticeListRequest;
import com.section.admin.notice.req.AdminOperationNoticeSaveRequest;
import com.section.admin.notice.res.AdminOperationNoticeDetailResponse;
import com.section.admin.notice.res.AdminOperationNoticeListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminOperationNoticeListQuery;
import com.section.common.system.dto.AdminOperationNoticeSummaryDto;
import com.section.common.system.entity.AdminOperationNotice;
import com.section.common.system.repository.AdminOperationNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOperationNoticeService {

    private final AdminOperationNoticeRepository adminOperationNoticeRepository;
    private final AdminLogService adminLogService;

    public AdminOperationNoticeListResponse getNoticeList(AdminOperationNoticeListRequest req) {
        AdminOperationNoticeListQuery query = req.toQuery();
        Page<com.section.common.system.dto.AdminOperationNoticeListResDto> page = adminOperationNoticeRepository.getNoticeList(
                query,
                PageRequest.of(req.normalizedPage(), req.normalizedSize())
        );
        AdminOperationNoticeSummaryDto summary = adminOperationNoticeRepository.getNoticeSummary(query, LocalDateTime.now());
        return AdminOperationNoticeListResponse.of(page, query, summary);
    }

    public AdminOperationNotice getNotice(Long noticeNo) {
        return adminOperationNoticeRepository.findById(noticeNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    public AdminOperationNoticeDetailResponse getNoticeDetail(Long noticeNo) {
        AdminOperationNotice notice = getNotice(noticeNo);

        AdminLogListRequest request = new AdminLogListRequest();
        request.setTargetId(noticeNo);
        request.setActionType("NOTICE_");
        AdminLogListResponse recentLogs = adminLogService.getLogList(request, PageRequest.of(0, 5));

        return AdminOperationNoticeDetailResponse.from(notice, recentLogs.items());
    }

    @Transactional
    public BulkOperateResult bulkOperate(AdminOperationNoticeBulkOperateRequest req) {
        req.validateOperation();
        List<Long> targetNoticeNos = req.normalizedNoticeNos();
        String normalizedActive = req.normalizedIsActive();
        String normalizedPinned = req.normalizedIsPinned();

        List<AdminOperationNotice> notices = adminOperationNoticeRepository.findAllById(targetNoticeNos);
        if (notices.isEmpty()) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }

        int updatedCount = 0;
        int unchangedCount = 0;
        for (AdminOperationNotice notice : notices) {
            boolean changed = false;
            if (normalizedActive != null && !normalizedActive.equalsIgnoreCase(notice.getIsActive())) {
                changed = true;
            }
            if (normalizedPinned != null && !normalizedPinned.equalsIgnoreCase(notice.getIsPinned())) {
                changed = true;
            }

            if (!changed) {
                unchangedCount += 1;
                continue;
            }

            notice.update(
                    notice.getTitle(),
                    notice.getContent(),
                    normalizedActive == null ? notice.getIsActive() : normalizedActive,
                    normalizedPinned == null ? notice.getIsPinned() : normalizedPinned,
                    notice.getStartDtm(),
                    notice.getEndDtm()
            );
            adminLogService.recordCurrentAdminLog("NOTICE_BULK_UPDATE", notice.getNoticeNo());
            updatedCount += 1;
        }

        return new BulkOperateResult(targetNoticeNos.size(), updatedCount, unchangedCount);
    }

    @Transactional
    public void saveNotice(AdminOperationNoticeSaveRequest req) {
        String normalizedActive = normalizeFlag(req.isActive(), "Y");
        String normalizedPinned = normalizeFlag(req.isPinned(), "N");
        String normalizedTitle = normalizeRequiredText(req.title());
        String normalizedContent = normalizeRequiredText(req.content());

        if (req.startDtm() != null && req.endDtm() != null && req.endDtm().isBefore(req.startDtm())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (req.noticeNo() == null) {
            AdminOperationNotice saved = adminOperationNoticeRepository.save(AdminOperationNotice.builder()
                    .title(normalizedTitle)
                    .content(normalizedContent)
                    .isActive(normalizedActive)
                    .isPinned(normalizedPinned)
                    .startDtm(req.startDtm())
                    .endDtm(req.endDtm())
                    .build());
            adminLogService.recordCurrentAdminLog("NOTICE_CREATE", saved.getNoticeNo());
            return;
        }

        AdminOperationNotice notice = getNotice(req.noticeNo());
        notice.update(
                normalizedTitle,
                normalizedContent,
                normalizedActive,
                normalizedPinned,
                req.startDtm(),
                req.endDtm()
        );
        adminLogService.recordCurrentAdminLog("NOTICE_UPDATE", notice.getNoticeNo());
    }

    @Transactional
    public void updateActive(Long noticeNo, String isActive) {
        AdminOperationNotice notice = getNotice(noticeNo);
        notice.updateActive(normalizeFlag(isActive, "Y"));
        adminLogService.recordCurrentAdminLog("NOTICE_ACTIVE_UPDATE", notice.getNoticeNo());
    }

    @Transactional
    public void deleteNotice(Long noticeNo) {
        adminOperationNoticeRepository.deleteById(noticeNo);
        adminLogService.recordCurrentAdminLog("NOTICE_DELETE", noticeNo);
    }

    private String normalizeFlag(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private String normalizeRequiredText(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    public record BulkOperateResult(
            int requestedCount,
            int updatedCount,
            int unchangedCount
    ) {
    }
}
