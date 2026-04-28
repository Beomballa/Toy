package com.section.common.content.service;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {
    private final DocumentRepository documentRepository;

    public Page<DocumentListItemDto> getDocumentList(DocumentListQuery query, Pageable pageable) {
        return documentRepository.getDocumentList(query, pageable);
    }

    public Document getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    @Transactional
    public Document readDocument(Long id) {
        Document document = getDocument(id);
        document.setViewCnt(document.getViewCnt() + 1);
        return document;
    }

    @Transactional
    public Document saveDocument(Document document) {
        return documentRepository.save(document);
    }

    @Transactional
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }
}
