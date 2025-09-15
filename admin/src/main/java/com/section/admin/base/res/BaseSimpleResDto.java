package com.section.admin.base.res;

import lombok.Data;

@Data
public class BaseSimpleResDto {

    protected String resultCode;

    protected String resultMsg;

    public BaseSimpleResDto() {
        this.resultCode = "200";
        this.resultMsg = "success";
    }

    public BaseSimpleResDto(String code, String resultMsg) {
        this.resultCode = code;
        this.resultMsg = resultMsg;
    }
}
