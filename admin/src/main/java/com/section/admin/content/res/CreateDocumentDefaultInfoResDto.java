package com.section.admin.content.res;

import com.section.common.content.entity.Document;
import com.section.common.util.DateUtil;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 추가
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더를 위한 전체 생성자 추가
public class CreateDocumentDefaultInfoResDto {
    private String docNo;
    private String title;
    private String content;
    private String uptDtm;
    private String viewYn;

    public static CreateDocumentDefaultInfoResDto fromDefaultInfo(Document document) {
        return CreateDocumentDefaultInfoResDto.builder()
                .docNo(String.valueOf(document.getApprovalDocument().getDocNo()))
                .title(document.getTitle())
                .uptDtm(DateUtil.localDateTimeToStr(document.getUptDtm()))
                .viewYn(document.getViewYn().toString())
                .build();
    }
 }
