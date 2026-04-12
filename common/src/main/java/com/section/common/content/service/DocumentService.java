package com.section.common.content.service;

import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {
    private final DocumentRepository documentRepository;

    public Page<Document> getDocumentList(Document.BoardType boardType, Pageable pageable) {
        return documentRepository.findAllByBoardTypeOrderByCrtDtmDesc(boardType, pageable);
    }

    public Document getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다. ID: " + id));
    }

    @Transactional
    public void saveDocument(Document document) {
        documentRepository.save(document);
    }

    @Transactional
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }
}
