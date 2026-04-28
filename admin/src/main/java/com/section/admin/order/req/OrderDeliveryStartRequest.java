package com.section.admin.order.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderDeliveryStartRequest(
        @NotNull(message = "주문 번호는 필수입니다.")
        Long orderNo,

        @NotBlank(message = "택배사는 필수입니다.")
        @Size(max = 50, message = "택배사는 50자 이하여야 합니다.")
        String deliveryCompany,

        @NotBlank(message = "운송장 번호는 필수입니다.")
        @Size(max = 50, message = "운송장 번호는 50자 이하여야 합니다.")
        String trackingNum
) {
    public String normalizedDeliveryCompany() {
        return this.deliveryCompany().trim();
    }

    public String normalizedTrackingNum() {
        // 운송장 번호는 외부 연동 키로 쓰일 수 있어 저장 전 공백을 제거합니다.
        return this.trackingNum().trim();
    }
}
