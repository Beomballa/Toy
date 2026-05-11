package com.section.common.commerce.repository;

import com.section.common.commerce.dto.BannerListQuery;
import com.section.common.commerce.dto.BannerListResDto;

import java.util.List;

public interface CustomBannerRepository {

    List<BannerListResDto> getBannerList(BannerListQuery query);
}
