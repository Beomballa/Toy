package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "display_banner")
public class DisplayBanner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_no")
    private Long bannerNo;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(name = "start_dtm", nullable = false)
    private LocalDateTime startDtm;

    @Column(name = "end_dtm", nullable = false)
    private LocalDateTime endDtm;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active", length = 1)
    private String isActive;

    @Column(name = "crt_admin_no", nullable = false)
    private Long crtAdminNo;

    public void update(String title, String imageUrl, String targetUrl, LocalDateTime startDtm, LocalDateTime endDtm, Integer sortOrder, String isActive) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.targetUrl = targetUrl;
        this.startDtm = startDtm;
        this.endDtm = endDtm;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
    }
}
