package com.section.front.memberaddress.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record FrontDeliveryAddressRequest(@NotBlank @Size(max=40) String addressName,@NotBlank @Size(max=50) String recipientName,@NotBlank @Size(max=20) String recipientPhone,@NotBlank @Size(max=10) String postalCode,@NotBlank @Size(max=200) String address1,@Size(max=200) String address2,boolean defaultAddress) {}
