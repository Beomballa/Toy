package com.section.admin.notice.service;

import com.section.admin.log.service.AdminLogService;
import com.section.admin.notice.req.AdminOperationNoticeListRequest;
import com.section.admin.notice.req.AdminOperationNoticeSaveRequest;
import com.section.admin.notice.res.AdminOperationNoticeListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminOperationNoticeListQuery;
import com.section.common.system.entity.AdminOperationNotice;
import com.section.common.system.repository.AdminOperationNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return AdminOperationNoticeListResponse.of(page, query);
    }

    public AdminOperationNotice getNotice(Long noticeNo) {
        return adminOperationNoticeRepository.findById(noticeNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
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
}
