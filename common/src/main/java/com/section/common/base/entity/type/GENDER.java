package com.section.common.base.entity.type;

import lombok.Getter;

@Getter
public enum GENDER {
    M("남자"), F("여자"), UNKNOWN("알수없음");

    private final String desc;

    GENDER(String desc) {this.desc = desc;}
}