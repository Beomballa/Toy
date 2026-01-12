package com.section.admin.content.res;

import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.entity.Document;
import com.section.common.system.entity.ApprovalDocument;
import com.section.common.util.DateUtil;
import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentMyDocResDto {
    private List<DocumentInfo> documents;

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DocumentInfo {
        private String docNo;
        private String title;
        private String content;
        private String uptDtm;
        private String viewYn;
    }

    public static ContentMyDocResDto fromEntity(Page<DocumentListItemDto> documents, List<ApprovalDocument> approvalDocuments) {
        List<DocumentInfo> documentInfos = documents.stream()
                .map(document -> DocumentInfo.builder()
                        .docNo(approvalDocuments.stream().map(ApprovalDocument::getDocNo).toString())
                        .title(document.getTitle() == null ? "제목없음" : document.getTitle())
                        .content(document.getContent())
                        .uptDtm(DateUtil.localDateTimeToStr(document.getUptDtm()))
                        .viewYn(document.getViewYn().toString())
                        .build())
                .collect(Collectors.toList());

        return ContentMyDocResDto.builder()
                .documents(documentInfos)
                .build();
    }
}