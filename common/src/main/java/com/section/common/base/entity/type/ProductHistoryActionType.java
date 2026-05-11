package com.section.common.base.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductHistoryActionType {
    CREATED("생성"),
    UPDATED("수정"),
    DELETED("삭제");

    private final String desc;
}
