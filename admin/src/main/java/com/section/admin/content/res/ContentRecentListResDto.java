package com.section.admin.content.res;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.entity.Document;
import com.section.common.util.DateUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ContentRecentListResDto {
    private List<ContentRecentResDto> list;

    public ContentRecentListResDto(List<Document> result) {
        this.list = result.stream().map(ContentRecentResDto::new).collect(Collectors.toList());
    }

    @Setter
    @Getter
    public class ContentRecentResDto {
        private Long docNo;
        private Long no;
        private String title;
        private String content;
        private String uptDtm;
        private String viewYn;

        private List<ContentRecentResDto> recentDocs;

        public ContentRecentResDto(Document item) {
            this.docNo = item.getApprovalDocument().getDocNo();
            this.no = item.getId(); // 값이 없다면 제외 가능
            this.title = item.getTitle();
            this.content = item.getContent();
            // 날짜 변환 (유틸 사용 예시)
            this.uptDtm = DateUtil.localDateTimeToStr(item.getUptDtm());
            // YN Enum 처리
            this.viewYn = (item.getViewYn() != null && item.getViewYn().equals(YN.Y)) ? "공개" : "비공개";
        }
    }
}
