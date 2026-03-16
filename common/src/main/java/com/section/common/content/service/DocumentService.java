package com.section.common.content.service;

import com.section.common.content.dto.ContentListItemDto;
import com.section.common.content.dto.DocumentListItemDto;
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

    public Page<DocumentListItemDto> findDocumentInfo(ContentListItemDto reqDto, Pageable pageable) {
        return documentRepository.findDocumentInfo(reqDto, pageable);
    }

}
