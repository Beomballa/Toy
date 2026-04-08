package com.section.common.commerce.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderListResDto {
    private Long orderNo;
    private String orderNum;
    private String buyerName;
    private String buyerPhone;
    private Integer totalAmount;
    private String status; // OrderStatus Enum이 아닌 String으로 변경
    private LocalDateTime crtDtm;
}
