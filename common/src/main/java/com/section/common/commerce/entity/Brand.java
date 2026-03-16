package com.section.common.commerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "BRAND")
public class Brand {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BRAND_NO")
    private Long id;

    @Column(name = "name_ko", nullable = false)
    private String nameKo;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "logo_url")
    private String logoUrl;
}
