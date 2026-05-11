package com.section.common.commerce.repository;

import com.section.common.commerce.entity.DisplayBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BannerRepository extends JpaRepository<DisplayBanner, Long>, CustomBannerRepository {
    List<DisplayBanner> findAllByOrderBySortOrderAscCrtDtmDesc();
}
