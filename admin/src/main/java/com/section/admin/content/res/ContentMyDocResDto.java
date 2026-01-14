package com.section.admin.content.res;

import com.section.common.base.entity.type.YN;
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
    private Long totalCount;

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DocumentInfo {
        private Long docNo;
        private String title;
        private String content;
        private String uptDtm;
        private String viewYn;
    }

    public static ContentMyDocResDto fromEntity(Page<DocumentListItemDto> documents, List<ApprovalDocument> approvalDocuments) {
        List<DocumentInfo> documentInfos = documents.stream()
                .map(document -> DocumentInfo.builder()
                        .docNo(document.getDocNo())
                        .title(document.getTitle() == null ? "" : document.getTitle())
                        .content(document.getContent() == null ? "" : document.getContent())
                        .uptDtm(DateUtil.localDateTimeToStr(document.getUptDtm()))
                        .viewYn(document.getViewYn().equals(YN.Y) ? "공개" : "비공개")
                        .build())
                .collect(Collectors.toList());

        return ContentMyDocResDto.builder()
                .documents(documentInfos)
                .totalCount(documents.getTotalElements())
                .build();
    }
}