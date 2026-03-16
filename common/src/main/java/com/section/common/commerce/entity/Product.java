package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "product")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_no")
    private Long id; // 보통 PK는 관례상 id로 쓰고 컬럼명만 지정합니다.

    // ✅ 연관 객체 대신 ID 컬럼만 직접 관리
    @Column(name = "category_no")
    private Long categoryNo;

    @Column(name = "brand_no")
    private Long brandNo;

    @Column(name = "name_ko", nullable = false, length = 200)
    private String nameKo;

    @Column(name = "model_num", length = 100)
    private String modelNum;

    @Column(name = "release_price")
    private Integer releasePrice;

    @Column(name = "release_dt")
    private LocalDate releaseDt;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "crt_admin_no")
    private Long crtAdminNo;

    public void updateBasicInfo(String nameKo, String modelNum, Integer releasePrice) {
        this.nameKo = nameKo;
        this.modelNum = modelNum;
        this.releasePrice = releasePrice;
    }

    public void changeStatus(String status) {
        this.status = status;
    }
}