package com.section.admin.base.req;

import lombok.Data;

@Data
public class PagingReqDto {
    protected int page;

    protected int pageSize;
}
