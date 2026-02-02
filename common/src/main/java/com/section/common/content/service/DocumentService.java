package com.section.common.content.service;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.ContentDateListItemDto;
import com.section.common.content.dto.ContentListItemDto;
import com.section.common.content.dto.DocumentDateListItemDto;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.system.entity.ApprovalDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {
    private final DocumentRepository documentRepository;

    public Page<DocumentListItemDto> findDocumentInfo(ContentListItemDto reqDto, Pageable pageable) {
        return documentRepository.findDocumentInfo(reqDto, pageable);
    }

    // 특정 기간 내 리스트 조회
    public List<DocumentDateListItemDto> findDocumentDateInfo(ContentDateListItemDto reqDto) {
        return documentRepository.findDocumentDateInfo(reqDto.getStartDt(), reqDto.getEndDt());
    }

    public Document createDocument(ApprovalDocument approvalDocument) {
        Document document = new Document();
        if(approvalDocument != null){
            document.setApprovalDocument(approvalDocument);
            document.setStatus("PR");
            document.setReserveYn(YN.N);
            document.setReserveDtm(LocalDateTime.now());
            document.setViewYn(YN.N);
            document.setCrtNo(approvalDocument.getCrtNo());
            documentRepository.save(document);
        }
        return document;
    }


}
