package com.section.admin.base.res;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class BaseListResDto extends BaseSimpleResDto{

    protected List<?> data;
    protected long totalCount;

    public BaseListResDto(List<?> data, long totalCount) {
        super();
        this.data = data;
        this.totalCount = totalCount;
    }

    public BaseListResDto(String code, String message) {
        this.resultCode = code;
        this.resultMsg = message;
    }
}
