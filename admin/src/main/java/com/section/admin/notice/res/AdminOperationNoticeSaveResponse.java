package com.section.admin.notice.res;

import com.section.admin.base.res.BaseSimpleResDto;

public class AdminOperationNoticeSaveResponse extends BaseSimpleResDto {

    private final Long noticeNo;

    public AdminOperationNoticeSaveResponse(Long noticeNo) {
        super();
        this.noticeNo = noticeNo;
    }

    public Long getNoticeNo() {
        return noticeNo;
    }
}
