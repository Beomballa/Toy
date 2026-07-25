package com.section.common.commerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_no")
    private Long id;

    @Column(name = "order_no", nullable = false)
    private Long orderNo;

    @Column(name = "product_no", nullable = false)
    private Long productNo;

    @Column(name = "option_no")
    private Long optionNo;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "order_price", nullable = false)
    private Integer orderPrice;

    @Column(name = "count", nullable = false)
    private Integer count;

    public static OrderItem create(
            Long orderNo,
            Long productNo,
            Long optionNo,
            String productName,
            int orderPrice,
            int count
    ) {
        return OrderItem.builder()
                .orderNo(orderNo)
                .productNo(productNo)
                .optionNo(optionNo)
                .productName(productName)
                .orderPrice(orderPrice)
                .count(count)
                .build();
    }
}
