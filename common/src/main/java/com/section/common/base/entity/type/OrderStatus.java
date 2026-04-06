package com.section.common.base.entity.type;

import lombok.Getter;

@Getter
public enum OrderStatus {
    ORDERED("주문완료"),
    PAID("결제완료"),
    PREPARING("배송준비"),
    SHIPPED("배송중"),
    DELIVERED("배송완료"),
    CANCELLED("주문취소");

    private final String desc;

    OrderStatus(String desc) {
        this.desc = desc;
    }
}
