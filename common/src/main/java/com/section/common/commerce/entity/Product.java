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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_no")
    private Long id;

    @Column(name = "category_no", nullable = false)
    private Long categoryNo;

    @Column(name = "brand_no", nullable = false)
    private Long brandNo;

    @Column(name = "name_ko", nullable = false)
    private String nameKo;

    @Column(name = "model_num")
    private String modelNum;

    private int releasePrice;
    private LocalDate releaseDt;
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Column(name = "crt_admin_no")
    private Long crtAdminNo;

    public enum ProductStatus { ACTIVE, HIDDEN, SOLD_OUT }
}