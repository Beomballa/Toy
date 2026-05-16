package com.section.common.base.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderHistoryOrderType {

    LATEST("latest", "최신순"),
    OLDEST("oldest", "오래된순");

    private final String code;
    private final String desc;
}
