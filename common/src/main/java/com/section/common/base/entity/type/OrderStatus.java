package com.section.common.base.entity.type;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

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

    public static Optional<OrderStatus> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(status -> status.name().equals(code))
                .findFirst();
    }

    public static String resolveDesc(String code) {
        return fromCode(code)
                .map(OrderStatus::getDesc)
                .orElse(code);
    }

    public boolean canCancel() {
        return this == ORDERED || this == PAID;
    }

    public boolean canStartDelivery() {
        return this == PAID;
    }

    public boolean canCompleteDelivery() {
        return this == SHIPPED;
    }

    public boolean showDeliveryInput() {
        return this == PAID;
    }

    public boolean showDeliveryInfo() {
        return this == SHIPPED || this == DELIVERED;
    }
}
