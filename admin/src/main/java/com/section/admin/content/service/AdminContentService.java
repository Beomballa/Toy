package com.section.admin.content.service;

import com.section.admin.content.req.ContentSetReqDto;
import com.section.admin.content.res.ContentGetResDto;
import com.section.admin.content.res.ContentMyDocResDto;
import com.section.admin.content.res.CreateDocumentDefaultInfoResDto;
import com.section.common.system.entity.Account;
import com.section.common.system.service.AdminAccountService;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.content.service.DocumentService;
import com.section.common.system.entity.ApprovalDocument;
import com.section.common.system.repository.ApprovalDocumentRepository;
import com.section.common.system.service.ApprovalDocumentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminContentService {

    private final ApprovalDocumentService approvalDocumentService;
    private final DocumentService documentService;
    private final AdminAccountService adminAccountService;

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final DocumentRepository documentRepository;

    public ContentMyDocResDto listDocument() {
        Account account = adminAccountService.findAccountInfo("wjdqjatnwkd@gmail.com", "1234");
        List<Document> document = documentService.findDocumentInfo(account.getCrtNo());
        return ContentMyDocResDto.fromEntity(document);
    }


    @Transactional
    public CreateDocumentDefaultInfoResDto setDocument() {
        ApprovalDocument approvalDocument = approvalDocumentService.createApprovalDocument();
        Document document = documentService.createDocument(approvalDocument);
        return CreateDocumentDefaultInfoResDto.fromDefaultInfo(document);
    }

    @Transactional
    public void setContent(ContentSetReqDto reqDto) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(Long.valueOf(reqDto.getDocNo()))
                .orElseThrow(() -> new EntityNotFoundException("해당 문서를 찾을 수 없습니다."));

        Document document = documentRepository.findByDocNo(Long.valueOf(reqDto.getDocNo()))
                        .orElseThrow(() -> new EntityNotFoundException("해당 콘텐츠 문서를 찾을 수 없습니다."));
        documentRepository.save(reqDto.toDocument(approvalDocument, document, reqDto));
    }

    /**
     * 문서 상세조회
     * @param docNoStr
     * */
    public ContentGetResDto getDocumentInfo(String docNoStr) {
        Long docNo = Long.valueOf(docNoStr);
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(docNo)
                .orElseThrow(() -> new EntityNotFoundException("해당 문서를 찾을 수 없습니다."));

        Document document = documentRepository.findByDocNo(docNo)
                .orElseThrow(() -> new EntityNotFoundException("해당 문서를 찾을 수 없습니다."));

        return ContentGetResDto.fromEntity(document, approvalDocument);
    }
}
