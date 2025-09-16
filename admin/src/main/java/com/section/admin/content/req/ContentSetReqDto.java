package com.section.admin.content.req;

import com.section.common.content.entity.Document;
import com.section.common.system.domain.SyDocument;
import com.section.common.system.entity.ApprovalDocument;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentSetReqDto implements SyDocument {

    private String title;
    private String content;

    private String contentTypeCode;
    private String status;
    private String reserveYn;
    private String reserveDtm;
    private String viewYn;

    public Document toDocument(ApprovalDocument approvalDocument) {
        Document document = new Document();
        document.setTitle(title);
        document.setContent(content);
//        document.set

        return document;
    }

    @Override
    public String getLang() {
        return "";
    }

    @Override
    public String getContentType() {
        return "";
    }

    @Override
    public Long getAdminNo() {
        return 0L;
    }
}
