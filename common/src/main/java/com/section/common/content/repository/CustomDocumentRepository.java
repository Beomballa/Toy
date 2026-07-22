package com.section.common.content.repository;

import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.dto.DocumentSummaryDto;
import com.section.common.content.dto.DocumentDailyStatsRow;
import com.section.common.content.dto.PublicDocumentRow;
import com.section.common.content.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CustomDocumentRepository {
    Page<DocumentListItemDto> getDocumentList(DocumentListQuery query, Pageable pageable);
    DocumentSummaryDto getDocumentSummary(DocumentListQuery query);
    List<DocumentDailyStatsRow> getDocumentDailyStats();
    List<PublicDocumentRow> getPublicDocuments(Document.BoardType boardType, int limit);
    Optional<PublicDocumentRow> getPublicDocument(long documentId);
}
