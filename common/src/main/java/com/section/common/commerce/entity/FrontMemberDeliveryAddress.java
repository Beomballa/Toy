package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "front_member_delivery_address")
public class FrontMemberDeliveryAddress extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_no") private Long id;
    @Column(name = "member_no", nullable = false) private Long memberNo;
    @Column(name = "address_name", nullable = false, length = 40) private String addressName;
    @Column(name = "recipient_name", nullable = false, length = 50) private String recipientName;
    @Column(name = "recipient_phone", nullable = false, length = 20) private String recipientPhone;
    @Column(name = "postal_code", nullable = false, length = 10) private String postalCode;
    @Column(name = "address1", nullable = false, length = 200) private String address1;
    @Column(name = "address2", length = 200) private String address2;
    @Column(name = "default_yn", nullable = false, length = 1) private String defaultYn;
    public static FrontMemberDeliveryAddress create(long memberNo, String name, String recipient, String phone, String postal, String address1, String address2, boolean isDefault) {
        FrontMemberDeliveryAddress address = new FrontMemberDeliveryAddress();
        address.memberNo=memberNo; address.addressName=name; address.recipientName=recipient; address.recipientPhone=phone; address.postalCode=postal; address.address1=address1; address.address2=address2; address.defaultYn=isDefault?"Y":"N";
        return address;
    }
    public boolean isDefaultAddress() { return "Y".equals(defaultYn); }
    public void setDefault(boolean value) { defaultYn = value ? "Y" : "N"; }
    public void update(String name, String recipient, String phone, String postal, String line1, String line2) { addressName=name; recipientName=recipient; recipientPhone=phone; postalCode=postal; address1=line1; address2=line2; }
}
