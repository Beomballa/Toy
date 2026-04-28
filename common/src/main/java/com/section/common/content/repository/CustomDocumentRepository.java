package com.section.common.content.repository;

import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomDocumentRepository {
    Page<DocumentListItemDto> getDocumentList(DocumentListQuery query, Pageable pageable);
}
