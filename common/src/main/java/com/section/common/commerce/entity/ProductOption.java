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
}