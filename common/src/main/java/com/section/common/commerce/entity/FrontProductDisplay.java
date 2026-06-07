package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "front_product_display")
public class FrontProductDisplay extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "display_no")
    private Long displayNo;

    @Column(name = "product_no", nullable = false, unique = true)
    private Long productNo;

    @Column(name = "headline", nullable = false, length = 120)
    private String headline;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "mood", nullable = false, length = 120)
    private String mood;

    @Builder.Default
    @Column(name = "featured_yn", nullable = false, length = 1)
    private String featuredYn = "N";

    @Builder.Default
    @Column(name = "featured_rank", nullable = false)
    private Integer featuredRank = 999;

    public boolean isFeatured() {
        return "Y".equalsIgnoreCase(featuredYn);
    }

    public void updateDisplay(String headline, String description, String mood, String featuredYn, Integer featuredRank) {
        this.headline = headline;
        this.description = description;
        this.mood = mood;
        this.featuredYn = featuredYn;
        this.featuredRank = featuredRank;
    }
}
