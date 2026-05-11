package com.section.admin.banner.service;

import com.section.admin.banner.req.BannerListRequest;
import com.section.admin.banner.req.BannerSaveRequest;
import com.section.admin.banner.res.BannerListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.BannerListQuery;
import com.section.common.commerce.dto.BannerListResDto;
import com.section.common.commerce.entity.DisplayBanner;
import com.section.common.commerce.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBannerService {

    private final BannerRepository bannerRepository;
    private final AdminBannerMapper adminBannerMapper;

    public BannerListResponse getBannerList(BannerListRequest req) {
        BannerListQuery query = req.toQuery();
        List<BannerListResDto> items = bannerRepository.getBannerList(query);
        return BannerListResponse.of(items, query);
    }

    public DisplayBanner getBanner(Long bannerNo) {
        return bannerRepository.findById(bannerNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Transactional
    public void saveBanner(BannerSaveRequest req) {
        if (!"Y".equalsIgnoreCase(req.isActive()) && !"N".equalsIgnoreCase(req.isActive())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (req.endDtm().isBefore(req.startDtm())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (req.bannerNo() == null) {
            bannerRepository.save(adminBannerMapper.toEntity(req));
            return;
        }

        DisplayBanner banner = getBanner(req.bannerNo());
        banner.update(
                req.title().trim(),
                req.imageUrl().trim(),
                req.targetUrl() == null ? null : req.targetUrl().trim(),
                req.startDtm(),
                req.endDtm(),
                req.sortOrder(),
                req.isActive().trim().toUpperCase()
        );
    }

    @Transactional
    public void updateActive(Long bannerNo, String isActive) {
        if (!"Y".equalsIgnoreCase(isActive) && !"N".equalsIgnoreCase(isActive)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        DisplayBanner banner = getBanner(bannerNo);
        banner.update(
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getTargetUrl(),
                banner.getStartDtm(),
                banner.getEndDtm(),
                banner.getSortOrder(),
                isActive.trim().toUpperCase()
        );
    }

    @Transactional
    public void deleteBanner(Long bannerNo) {
        bannerRepository.deleteById(bannerNo);
    }
}
