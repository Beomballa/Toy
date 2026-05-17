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

import java.util.Set;

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
        if (document.getId() == null) {
            return documentRepository.save(document);
        }

        Document existingDocument = getDocument(document.getId());
        // 수정 시에는 기존 엔티티를 그대로 유지하고 변경 필드만 덮어서 조회수/감사값이 흔들리지 않게 한다.
        existingDocument.applyEditorValues(
                document.getBoardType(),
                document.getStatus(),
                document.getPublicYn(),
                document.getPinnedYn(),
                document.getTitle(),
                document.getContent(),
                document.getProductNo()
        );
        return existingDocument;
    }

    @Transactional
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }

    @Transactional
    public BulkOperateResult bulkOperateDocuments(
            Set<Long> ids,
            Document.PublishStatus status,
            com.section.common.base.entity.type.YN publicYn,
            com.section.common.base.entity.type.YN pinnedYn
    ) {
        int updatedCount = 0;
        for (Long id : ids) {
            Document document = getDocument(id);
            if (document.applyOperateValues(status, publicYn, pinnedYn)) {
                updatedCount += 1;
            }
        }
        return BulkOperateResult.of(ids.size(), updatedCount);
    }

    public record BulkOperateResult(
            int requestedCount,
            int updatedCount,
            int unchangedCount
    ) {
        public static BulkOperateResult of(int requestedCount, int updatedCount) {
            return new BulkOperateResult(requestedCount, updatedCount, requestedCount - updatedCount);
        }
    }
}
