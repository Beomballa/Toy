package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(access = AccessLevel.PRIVATE)
@Table(name = "order_delivery")
public class OrderDelivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_delivery_no")
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true)
    private Long orderNo;

    @Column(name = "recipient_name", nullable = false, length = 50)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(name = "address1", nullable = false, length = 200)
    private String address1;

    @Column(name = "address2", length = 200)
    private String address2;

    @Column(name = "delivery_request", length = 200)
    private String deliveryRequest;

    public static OrderDelivery create(
            Long orderNo,
            String recipientName,
            String recipientPhone,
            String postalCode,
            String address1,
            String address2,
            String deliveryRequest
    ) {
        return OrderDelivery.builder()
                .orderNo(orderNo)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .postalCode(postalCode)
                .address1(address1)
                .address2(address2)
                .deliveryRequest(deliveryRequest)
                .build();
    }
}
