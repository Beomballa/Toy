package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "order_status_history")
public class OrderStatusHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_no")
    private Long id;

    @Column(name = "order_no", nullable = false)
    private Long orderNo;

    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    @Column(name = "before_status", length = 20)
    private String beforeStatus;

    @Column(name = "after_status", length = 20)
    private String afterStatus;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "admin_memo_snapshot", length = 1000)
    private String adminMemoSnapshot;

    @Column(name = "delivery_company", length = 50)
    private String deliveryCompany;

    @Column(name = "tracking_num", length = 50)
    private String trackingNum;

    public static OrderStatusHistory create(
            Long orderNo,
            String actionType,
            String beforeStatus,
            String afterStatus,
            String reason,
            String adminMemoSnapshot,
            String deliveryCompany,
            String trackingNum
    ) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.orderNo = orderNo;
        history.actionType = actionType;
        history.beforeStatus = beforeStatus;
        history.afterStatus = afterStatus;
        history.reason = reason;
        history.adminMemoSnapshot = adminMemoSnapshot;
        history.deliveryCompany = deliveryCompany;
        history.trackingNum = trackingNum;
        return history;
    }
}
