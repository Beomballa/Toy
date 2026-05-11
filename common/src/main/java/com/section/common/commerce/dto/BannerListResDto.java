package com.section.common.commerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BannerListResDto {

    private Long bannerNo;
    private String title;
    private String imageUrl;
    private String targetUrl;
    private LocalDateTime startDtm;
    private LocalDateTime endDtm;
    private Integer sortOrder;
    private String isActive;
}
