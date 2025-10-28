package com.section.common.content.custom;

import com.section.common.content.dto.ContentListItemDto;
import com.section.common.content.dto.DocumentListItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomDocumentRepository {
    Page<DocumentListItemDto> findAllDocumentInfo(ContentListItemDto reqDto, Pageable pageable);
}
