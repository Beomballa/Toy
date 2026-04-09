package com.section.common.commerce.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResDto {
    private Long orderItemNo;
    private Long productNo;
    private String productName;
    private Integer orderPrice;
    private Integer count;
    private String thumbnailUrl;
}
