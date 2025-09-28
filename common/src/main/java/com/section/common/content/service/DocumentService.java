package com.section.common.content.service;

import com.section.common.base.entity.type.YN;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.system.entity.ApprovalDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {
    private final DocumentRepository documentRepository;

    public List<Document> findDocumentInfo(String adminNo) {
        return documentRepository.findByDocumentInfo(adminNo);
    }

    public Document createDocument(ApprovalDocument approvalDocument) {
        Document document = new Document();
        if(approvalDocument != null){
            document.setApprovalDocument(approvalDocument);
            document.setStatus("PR");
            document.setViewYn(YN.N);
            document.setCrtNo(approvalDocument.getCrtNo());
            documentRepository.save(document);
        }
        return document;
    }


}
