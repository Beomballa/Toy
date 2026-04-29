package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
@Table(name = "orders")
public class Orders extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_no")
    private Long id;

    @Column(name = "order_num", nullable = false, unique = true, length = 50)
    private String orderNum;

    @Column(name = "buyer_name", nullable = false, length = 50)
    private String buyerName;

    @Column(name = "buyer_phone", nullable = false, length = 20)
    private String buyerPhone;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "delivery_company", length = 50)
    private String deliveryCompany;

    @Column(name = "tracking_num", length = 50)
    private String trackingNum;

    /**
     * 주문 생성 (정적 팩토리 메서드)
     */
    public static Orders createOrder(String orderNum, String buyerName, String buyerPhone, Integer totalAmount) {
        return Orders.builder()
                .orderNum(orderNum)
                .buyerName(buyerName)
                .buyerPhone(buyerPhone)
                .totalAmount(totalAmount)
                .status(OrderStatus.ORDERED.name())
                .build();
    }

    /**
     * 상태 변경 비즈니스 메서드
     */
    public void changeStatus(OrderStatus newStatus) {
        this.status = newStatus.name();
    }

    public void pay() {
        this.status = OrderStatus.PAID.name();
    }

    /**
     * 배송 시작 처리
     */
    public void startDelivery(String deliveryCompany, String trackingNum) {
        if (!OrderStatus.PAID.name().equals(this.status)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }
        this.deliveryCompany = deliveryCompany;
        this.trackingNum = trackingNum;
        this.status = OrderStatus.SHIPPED.name();
    }

    /**
     * 배송 완료 처리
     */
    public void completeDelivery() {
        if (!OrderStatus.SHIPPED.name().equals(this.status)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }
        this.status = OrderStatus.DELIVERED.name();
    }

    /**
     * 주문 취소 처리
     */
    public void cancel() {
        if (OrderStatus.SHIPPED.name().equals(this.status) || OrderStatus.DELIVERED.name().equals(this.status)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }
        this.status = OrderStatus.CANCELLED.name();
    }
}
