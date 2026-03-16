//package com.section.admin.content.req;
//
//import com.section.common.base.entity.type.YN;
//import com.section.common.content.entity.Document;
//import com.section.common.system.domain.SyDocument;
//import com.section.common.system.entity.ApprovalDocument;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.Pattern;
//import lombok.Getter;
//import lombok.Setter;
//
//import java.time.LocalDateTime;
//
//@Getter
//@Setter
//public class ContentSetReqDto implements SyDocument {
//
//    private String docNo;
//    private String title;
//    private String content;
//
//    private String contentTypeCode;
//
////    @NotBlank(message = "해당 게시글의 공개여부를 선택해주세요.")
////    @Pattern(regexp = "^(RE|PR|NT)$", message = "공개여부는 필수 선택 값입니다.")
//    private String status;
//    private String reserveYn;
//    private String reserveDtm;
//    private String viewYn;
//
//    public Document toDocument(ApprovalDocument approvalDocument, Document document, ContentSetReqDto dto) {
//        document.setApprovalDocument(approvalDocument);
//        document.setTitle(dto.getTitle());
//        document.setContent(dto.getContent());
//        document.setStatus("PR");
//        document.setReserveYn(YN.N);
////        document.setReserveYn(YN.valueOf(reserveYn));
//        document.setReserveDtm(LocalDateTime.now());
////        document.setViewYn(YN.valueOf(viewYn));
//        return document;
//    }
//    // 메서드 명 변경 및 파라미터 간소화
//    public void updateDocument(Document document, ApprovalDocument approvalDocument) {
//        document.setApprovalDocument(approvalDocument);
//        document.setTitle(this.title);       // this 사용
//        document.setContent(this.content);   // this 사용
//        document.setStatus("PR");
//        document.setReserveYn(YN.N);
//        document.setReserveDtm(LocalDateTime.now());
//    }
//
//    @Override
//    public String getLang() {
//        return "KO";
//    }
//
//    @Override
//    public String getContentType() {
//        return "";
//    }
//
//    @Override
//    public Long getAdminNo() {
//        return 0L;
//    }
//}
