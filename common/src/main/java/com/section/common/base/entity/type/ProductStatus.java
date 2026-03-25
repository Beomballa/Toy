package com.section.common.base.entity.type;

import lombok.Getter;

@Getter
public enum ProductStatus {
    ACTIVE("판매중"),
    HIDDEN("숨김"),
    SOLD_OUT("품절"),
    DELETE("삭제");

    private final String desc;

    ProductStatus(String desc) {this.desc = desc;}
}
