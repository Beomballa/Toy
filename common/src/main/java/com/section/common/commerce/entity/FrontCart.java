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
@Table(name = "front_cart")
public class FrontCart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_no")
    private Long id;

    @Column(name = "cart_token", nullable = false, unique = true, length = 80)
    private String cartToken;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    public static FrontCart create(String cartToken) {
        return FrontCart.builder()
                .cartToken(cartToken)
                .status("ACTIVE")
                .build();
    }

    public void complete() {
        this.status = "ORDERED";
    }
}
