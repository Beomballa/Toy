package com.section.admin.content.service;

import com.section.admin.content.req.ContentSetReqDto;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.system.service.ApprovalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminContentService {

//    private ApprovalDocumentService approvalDocumentService;

    private final DocumentRepository documentRepository;

    @Transactional
    public void setContent(ContentSetReqDto reqDto) {
//        ApprovalDocument approvalDocument = approvalDocumentService.createApprovalDocument(reqDto.toDocument());
        documentRepository.save(reqDto.toDocument());
    }
}
