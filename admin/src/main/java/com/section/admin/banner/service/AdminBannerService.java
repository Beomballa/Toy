package com.section.admin.banner.service;

import com.section.admin.banner.req.BannerBulkDeleteRequest;
import com.section.admin.banner.req.BannerBulkOperateRequest;
import com.section.admin.banner.req.BannerListRequest;
import com.section.admin.banner.req.BannerSaveRequest;
import com.section.admin.banner.res.BannerDetailResponse;
import com.section.admin.banner.res.BannerListResponse;
import com.section.admin.banner.support.BannerExportCsvWriter;
import com.section.admin.banner.support.BannerExportSummary;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.BannerListQuery;
import com.section.common.commerce.dto.BannerListResDto;
import com.section.common.commerce.entity.DisplayBanner;
import com.section.common.commerce.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBannerService {
    private static final int BANNER_EXPORT_MAX_SIZE = 1000;

    private final BannerRepository bannerRepository;
    private final AdminBannerMapper adminBannerMapper;

    public BannerListResponse getBannerList(BannerListRequest req) {
        BannerListQuery query = req.toQuery();
        Page<BannerListResDto> page = bannerRepository.getBannerList(
                query,
                PageRequest.of(req.normalizedPage(), req.normalizedSize())
        );
        return BannerListResponse.of(page, query);
    }

    public byte[] exportBannerListCsv(BannerListRequest req) {
        BannerListQuery query = req.toQuery();
        List<BannerListResDto> items = bannerRepository.getBannerList(
                query,
                PageRequest.of(0, BANNER_EXPORT_MAX_SIZE)
        ).getContent();
        return BannerExportCsvWriter.write(BannerExportSummary.from(query), items);
    }

    public DisplayBanner getBanner(Long bannerNo) {
        return bannerRepository.findById(bannerNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    public BannerDetailResponse getBannerDetail(Long bannerNo) {
        return BannerDetailResponse.from(getBanner(bannerNo));
    }

    @Transactional
    public void saveBanner(BannerSaveRequest req) {
        BannerSaveRequest normalizedRequest = normalize(req);
        validateScheduleConflict(normalizedRequest);

        if (normalizedRequest.bannerNo() == null) {
            bannerRepository.save(adminBannerMapper.toEntity(normalizedRequest));
            return;
        }

        DisplayBanner banner = getBanner(normalizedRequest.bannerNo());
        banner.update(
                normalizedRequest.title(),
                normalizedRequest.imageUrl(),
                normalizedRequest.targetUrl(),
                normalizedRequest.startDtm(),
                normalizedRequest.endDtm(),
                normalizedRequest.sortOrder(),
                normalizedRequest.isActive()
        );
    }

    @Transactional
    public void updateActive(Long bannerNo, String isActive) {
        String normalizedIsActive = normalizeFlag(isActive);
        DisplayBanner banner = getBanner(bannerNo);
        validateScheduleConflict(banner.getBannerNo(), banner.getSortOrder(), banner.getStartDtm(), banner.getEndDtm(), normalizedIsActive);
        banner.update(
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getTargetUrl(),
                banner.getStartDtm(),
                banner.getEndDtm(),
                banner.getSortOrder(),
                normalizedIsActive
        );
    }

    @Transactional
    public BulkOperateResult bulkOperate(BannerBulkOperateRequest req) {
        req.validateOperation();
        List<Long> targetBannerNos = req.normalizedBannerNos();
        String normalizedIsActive = req.normalizedIsActive();

        List<DisplayBanner> banners = bannerRepository.findAllById(targetBannerNos);
        if (banners.isEmpty()) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }

        int updatedCount = 0;
        int unchangedCount = 0;
        for (DisplayBanner banner : banners) {
            if (normalizedIsActive.equalsIgnoreCase(banner.getIsActive())) {
                unchangedCount += 1;
                continue;
            }
            validateScheduleConflict(banner.getBannerNo(), banner.getSortOrder(), banner.getStartDtm(), banner.getEndDtm(), normalizedIsActive);
            banner.update(
                    banner.getTitle(),
                    banner.getImageUrl(),
                    banner.getTargetUrl(),
                    banner.getStartDtm(),
                    banner.getEndDtm(),
                    banner.getSortOrder(),
                    normalizedIsActive
            );
            updatedCount += 1;
        }

        return new BulkOperateResult(targetBannerNos.size(), updatedCount, unchangedCount);
    }

    @Transactional
    public void deleteBanner(Long bannerNo) {
        DisplayBanner banner = getBanner(bannerNo);
        bannerRepository.delete(banner);
    }

    @Transactional
    public BulkDeleteResult bulkDelete(BannerBulkDeleteRequest req) {
        List<Long> targetBannerNos = req.normalizedBannerNos();
        List<DisplayBanner> banners = bannerRepository.findAllById(targetBannerNos);
        if (banners.isEmpty()) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }

        bannerRepository.deleteAll(banners);
        List<Long> existingBannerNos = banners.stream()
                .map(DisplayBanner::getBannerNo)
                .toList();
        HashSet<Long> existingBannerNoSet = new HashSet<>(existingBannerNos);
        long missingCount = targetBannerNos.stream()
                .filter(no -> !existingBannerNoSet.contains(no))
                .count();
        return new BulkDeleteResult(targetBannerNos.size(), banners.size(), (int) missingCount);
    }

    private BannerSaveRequest normalize(BannerSaveRequest req) {
        String normalizedTitle = normalizeRequiredText(req.title());
        String normalizedImageUrl = normalizeRequiredText(req.imageUrl());
        String normalizedTargetUrl = normalizeOptionalText(req.targetUrl());
        String normalizedIsActive = normalizeFlag(req.isActive());

        if (req.startDtm() == null || req.endDtm() == null || req.endDtm().isBefore(req.startDtm())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return new BannerSaveRequest(
                req.bannerNo(),
                normalizedTitle,
                normalizedImageUrl,
                normalizedTargetUrl,
                req.startDtm(),
                req.endDtm(),
                req.sortOrder(),
                normalizedIsActive
        );
    }

    private void validateScheduleConflict(BannerSaveRequest req) {
        validateScheduleConflict(req.bannerNo(), req.sortOrder(), req.startDtm(), req.endDtm(), req.isActive());
    }

    private void validateScheduleConflict(Long bannerNo, Integer sortOrder, java.time.LocalDateTime startDtm, java.time.LocalDateTime endDtm, String isActive) {
        if (!"Y".equals(isActive)) {
            return;
        }
        if (bannerRepository.existsActiveBannerScheduleConflict(bannerNo, sortOrder, startDtm, endDtm)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizeFlag(String value) {
        if (!"Y".equalsIgnoreCase(value) && !"N".equalsIgnoreCase(value)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return value.trim().toUpperCase();
    }

    private String normalizeRequiredText(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    public record BulkOperateResult(
            int requestedCount,
            int updatedCount,
            int unchangedCount
    ) {
    }

    public record BulkDeleteResult(
            int requestedCount,
            int deletedCount,
            int missingCount
    ) {
    }
}
