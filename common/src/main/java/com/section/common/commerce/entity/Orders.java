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

    @Column(name = "member_no")
    private Long memberNo;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "delivery_company", length = 50)
    private String deliveryCompany;

    @Column(name = "tracking_num", length = 50)
    private String trackingNum;

    @Column(name = "admin_memo", length = 1000)
    private String adminMemo;

    /**
     * 주문 생성 (정적 팩토리 메서드)
     */
    public static Orders createOrder(String orderNum, String buyerName, String buyerPhone, Integer totalAmount) {
        return createOrder(orderNum, buyerName, buyerPhone, totalAmount, null);
    }

    public static Orders createOrder(
            String orderNum,
            String buyerName,
            String buyerPhone,
            Integer totalAmount,
            Long memberNo
    ) {
        return Orders.builder()
                .orderNum(orderNum)
                .buyerName(buyerName)
                .buyerPhone(buyerPhone)
                .totalAmount(totalAmount)
                .memberNo(memberNo)
                .status(OrderStatus.ORDERED.name())
                .build();
    }

    /**
     * 상태 변경 비즈니스 메서드
     */
    public void changeStatus(OrderStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }
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
        if (OrderStatus.SHIPPED.name().equals(this.status)
                || OrderStatus.DELIVERED.name().equals(this.status)
                || OrderStatus.CANCELLED.name().equals(this.status)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }
        this.status = OrderStatus.CANCELLED.name();
    }

    public void updateAdminMemo(String adminMemo) {
        this.adminMemo = adminMemo;
    }

    private boolean canTransitionTo(OrderStatus newStatus) {
        OrderStatus currentStatus = OrderStatus.valueOf(this.status);

        // 같은 상태 재적용은 운영자가 화면 동기화를 다시 맞출 때 불필요한 실패를 만들지 않도록 허용합니다.
        if (currentStatus == newStatus) {
            return true;
        }

        return switch (currentStatus) {
            case ORDERED -> newStatus == OrderStatus.PAID || newStatus == OrderStatus.CANCELLED;
            case PAID -> newStatus == OrderStatus.PREPARING
                    || newStatus == OrderStatus.SHIPPED
                    || newStatus == OrderStatus.CANCELLED;
            case PREPARING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
