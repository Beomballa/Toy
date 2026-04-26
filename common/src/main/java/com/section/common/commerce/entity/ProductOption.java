package com.section.common.commerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "product_option")
public class ProductOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_no")
    private Long id;

    @Column(name = "product_no", nullable = false)
    private Long productNo;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(name = "stock_cnt", nullable = false)
    private Integer stockCnt;

    @Column(name = "additional_price", nullable = false)
    private Integer additionalPrice;

    /**
     * 재고 추가
     */
    public void addStock(int quantity) {
        this.stockCnt += quantity;
    }

    /**
     * 재고 감소
     */
    public void removeStock(int quantity) {
        int restStock = this.stockCnt - quantity;
        if (restStock < 0) {
            throw new IllegalStateException("재고가 부족합니다. (현재 재고: " + this.stockCnt + ")");
        }
        this.stockCnt = restStock;
    }

    /**
     * 옵션 정보 수정
     */
    public void updateOption(String optionName, Integer stockCnt, Integer additionalPrice) {
        this.optionName = optionName;
        this.stockCnt = stockCnt;
        this.additionalPrice = additionalPrice;
    }
}