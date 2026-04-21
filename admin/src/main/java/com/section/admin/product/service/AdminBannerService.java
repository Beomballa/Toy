package com.section.admin.product.service;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
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

    public List<DisplayBanner> getBannerList() {
        return bannerRepository.findAllByOrderBySortOrderAscCrtDtmDesc();
    }

    public DisplayBanner getBanner(Long bannerNo) {
        return bannerRepository.findById(bannerNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Transactional
    public void saveBanner(DisplayBanner banner) {
        // 임시로 관리자 번호 1번 고정
        if (banner.getBannerNo() == null) {
            // 신규 등록 시 처리
        }
        bannerRepository.save(banner);
    }

    @Transactional
    public void deleteBanner(Long bannerNo) {
        bannerRepository.deleteById(bannerNo);
    }
}
