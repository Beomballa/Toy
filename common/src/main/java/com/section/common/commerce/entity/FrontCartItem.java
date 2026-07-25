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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
@Table(name = "front_cart_item")
public class FrontCartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_no")
    private Long id;

    @Column(name = "cart_no", nullable = false)
    private Long cartNo;

    @Column(name = "product_no", nullable = false)
    private Long productNo;

    @Column(name = "option_no", nullable = false)
    private Long optionNo;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public static FrontCartItem create(Long cartNo, Long productNo, Long optionNo, int quantity) {
        return FrontCartItem.builder()
                .cartNo(cartNo)
                .productNo(productNo)
                .optionNo(optionNo)
                .quantity(quantity)
                .build();
    }

    public void changeQuantity(int quantity) {
        this.quantity = quantity;
    }
}
