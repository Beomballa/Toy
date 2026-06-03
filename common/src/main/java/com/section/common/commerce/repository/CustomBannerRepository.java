package com.section.common.commerce.repository;

import com.section.common.commerce.dto.BannerListQuery;
import com.section.common.commerce.dto.BannerListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface CustomBannerRepository {

    Page<BannerListResDto> getBannerList(BannerListQuery query, Pageable pageable);

    boolean existsActiveBannerScheduleConflict(Long bannerNo, Integer sortOrder, LocalDateTime startDtm, LocalDateTime endDtm);
}
