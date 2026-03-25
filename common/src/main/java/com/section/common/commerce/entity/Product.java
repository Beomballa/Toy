package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.ProductCreateReqDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
@Table(name = "product")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_no")
    private Long id;

    @Column(name = "category_no", nullable = false)
    private Long categoryNo;

    @Column(name = "brand_no", nullable = false)
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

    /**
     * 상품 생성
     *
     * @return Product
     * @Param reqDto
     * @Param adminNo
     *
     */
    public static Product createProduct(ProductCreateReqDto reqDto) {
        return Product.builder()
                .categoryNo(reqDto.getCategoryNo())
                .brandNo(reqDto.getBrandNo())
                .nameKo(reqDto.getNameKo())
                .modelNum(reqDto.getModelNum())
                .releasePrice(reqDto.getReleasePrice())
                .releaseDt(reqDto.getReleaseDt())
                .thumbnailUrl(reqDto.getThumbnailUrl())
                .status(ProductStatus.ACTIVE.name())
                .build();
    }

    /**
     * 상품 기본 정보 수정
     * */
    public void updateBasicInfo(String nameKo, String modelNum, Integer releasePrice, LocalDate releaseDt, String thumbnailUrl) {
        this.nameKo = nameKo;
        this.modelNum = modelNum;
        this.releasePrice = releasePrice;
        this.releaseDt = releaseDt;
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * 카테고리 변경
     */
    public void changeCategory(Long categoryNo) {
        this.categoryNo = categoryNo;
    }

    /**
     * 브랜드 변경
     */
    public void changeBrand(Long brandNo) {
        this.brandNo = brandNo;
    }

    /**
     * 상태 변경
     * */
    public void changeStatus(ProductStatus status) {
        this.status = status.name();
    }

    /**
     * 상품 활성화
     */
    public void activate() {
        this.status = ProductStatus.ACTIVE.name();
    }

    /**
     * 상품 숨김 처리
     */
    public void hide() {
        this.status = ProductStatus.HIDDEN.name();
    }

    /**
     * 품절 처리
     */
    public void soldOut() {
        this.status = ProductStatus.SOLD_OUT.name();
    }

    /**
     * 판매중인지 확인
     */
    public boolean isActive() {
        return ProductStatus.ACTIVE.name().equals(this.status);
    }

    /**
     * 품절 상태인지 확인
     */
    public boolean isSoldOut() {
        return ProductStatus.SOLD_OUT.name().equals(this.status);
    }

    public void deleteProduct() {
        this.status = ProductStatus.DELETE.name();
    }
}