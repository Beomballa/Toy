package com.section.front.memberaddress.dto;
import com.section.common.commerce.entity.FrontMemberDeliveryAddress;
public record FrontDeliveryAddressResponse(long id,String addressName,String recipientName,String recipientPhone,String postalCode,String address1,String address2,boolean defaultAddress) {
    public static FrontDeliveryAddressResponse from(FrontMemberDeliveryAddress a){return new FrontDeliveryAddressResponse(a.getId(),a.getAddressName(),a.getRecipientName(),a.getRecipientPhone(),a.getPostalCode(),a.getAddress1(),a.getAddress2(),a.isDefaultAddress());}
}
