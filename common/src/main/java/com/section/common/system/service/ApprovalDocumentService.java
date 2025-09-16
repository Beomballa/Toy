package com.section.common.system.service;

import com.section.common.content.entity.Document;
import com.section.common.system.entity.ApprovalDocument;
import com.section.common.system.repository.ApprovalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalDocumentService {

    private final ApprovalDocumentRepository approvalDocumentRepository;

    /**
     * 결재 문서 생성
     * @param document
     * @return
     */
    @Transactional
    public ApprovalDocument createApprovalDocument(Document document) {
        ApprovalDocument approvalDocument = new ApprovalDocument();
        return approvalDocumentRepository.save(approvalDocument);
    }

//    private String getDocumentTitle(Document document) {
//        String title;
//        if(StringUtils.hasText(document.getTitle())) {
//            title = document.getTitle();
//        } else {
//            Optional<ContentT>
//        }
//    }
}
