package com.section.admin.content.service;

import com.section.admin.content.req.ContentListReqDto;
import com.section.admin.content.req.ContentSetReqDto;
import com.section.admin.content.res.ContentGetResDto;
import com.section.admin.content.res.ContentMyDocResDto;
import com.section.admin.content.res.CreateDocumentDefaultInfoResDto;
import com.section.common.content.dto.DocumentListItemDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    public ContentMyDocResDto listDocument(ContentListReqDto reqDto) {
        Account currentAccount = adminAccountService.findAccountInfo("wjdqjatnwkd@gmail.com", "1234")
                .orElseThrow(() -> new EntityNotFoundException("계정 정보를 찾을 수 없습니다."));

        // 조회 대상이 되는 리스트 조회
        Page<DocumentListItemDto> result = documentService.findDocumentInfo(reqDto.toContentListItemDto(currentAccount), PageRequest.of(reqDto.getPage(), reqDto.getPageSize()));

        // 해당 테이블에 저장된
        List<Long> ids = result.stream()
                .map(DocumentListItemDto::getDocNo)
                .toList();

        // 원본 문서에 저장된 정보 조회
        List<ApprovalDocument> approvalDocuments = approvalDocumentRepository.findApprovalDocumentInfo(ids);

        return ContentMyDocResDto.fromEntity(result, approvalDocuments);
    }


    /**
     * 문서 생성
     * */
    @Transactional
    public CreateDocumentDefaultInfoResDto setDocument() {
        ApprovalDocument approvalDocument = approvalDocumentService.createApprovalDocument();
        Document document = documentService.createDocument(approvalDocument);
        return CreateDocumentDefaultInfoResDto.fromDefaultInfo(document);
    }

    /**
     * 문서 작성
     * */
    @Transactional
    public void setContent(ContentSetReqDto reqDto) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(Long.valueOf(reqDto.getDocNo()))
                .orElseThrow(() -> new EntityNotFoundException("해당 문서를 찾을 수 없습니다."));

        Document document = documentRepository.findByDocNo(Long.valueOf(reqDto.getDocNo()))
                        .orElseThrow(() -> new EntityNotFoundException("해당 콘텐츠 문서를 찾을 수 없습니다."));
        reqDto.updateDocument(document, approvalDocument);
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
