package com.section.common.content.service;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.base.entity.type.YN;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.dto.DocumentSummaryDto;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.content.repository.FrontContentViewEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final ProductRepository productRepository;
    private final FrontContentViewEventRepository viewEventRepository;

    public Page<DocumentListItemDto> getDocumentList(DocumentListQuery query, Pageable pageable) {
        return documentRepository.getDocumentList(query, pageable);
    }

    public DocumentSummaryDto getDocumentSummary(DocumentListQuery query) {
        return documentRepository.getDocumentSummary(query);
    }

    public List<DocumentListItemDto> getDocumentExportList(DocumentListQuery query, int limit) {
        return documentRepository.getDocumentList(query, Pageable.ofSize(limit)).getContent();
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
        validateProductReference(document.getProductNo());
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
        Document document = getDocument(id);
        viewEventRepository.deleteByDocumentNo(document.getId());
        documentRepository.delete(document);
    }

    @Transactional
    public BulkOperateResult operateDocument(
            Long id,
            Document.PublishStatus status,
            YN publicYn,
            YN pinnedYn
    ) {
        Document document = getDocument(id);
        int updatedCount = document.applyOperateValues(status, publicYn, pinnedYn) ? 1 : 0;
        return BulkOperateResult.of(1, updatedCount, 0);
    }

    @Transactional
    public BulkOperateResult bulkOperateDocuments(
            Set<Long> ids,
            Document.PublishStatus status,
            YN publicYn,
            YN pinnedYn
    ) {
        List<Document> documents = documentRepository.findAllById(ids);
        if (documents.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        Set<Long> existingIds = documents.stream()
                .map(Document::getId)
                .collect(java.util.stream.Collectors.toSet());
        int updatedCount = 0;
        for (Document document : documents) {
            if (document.applyOperateValues(status, publicYn, pinnedYn)) {
                updatedCount += 1;
            }
        }
        long missingCount = ids.stream()
                .filter(id -> !existingIds.contains(id))
                .count();
        return BulkOperateResult.of(ids.size(), updatedCount, (int) missingCount);
    }

    @Transactional
    public BulkDeleteResult bulkDeleteDocuments(Set<Long> ids) {
        List<Document> documents = documentRepository.findAllById(ids);
        if (documents.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        Set<Long> deletedIds = documents.stream()
                .map(Document::getId)
                .collect(java.util.stream.Collectors.toSet());
        viewEventRepository.deleteByDocumentNoIn(deletedIds);
        documentRepository.deleteAll(documents);
        long missingCount = ids.stream()
                .filter(id -> !deletedIds.contains(id))
                .count();

        return new BulkDeleteResult(ids.size(), documents.size(), (int) missingCount);
    }

    public record BulkOperateResult(
            int requestedCount,
            int updatedCount,
            int unchangedCount,
            int missingCount
    ) {
        public static BulkOperateResult of(int requestedCount, int updatedCount, int missingCount) {
            return new BulkOperateResult(
                    requestedCount,
                    updatedCount,
                    Math.max(requestedCount - updatedCount - missingCount, 0),
                    missingCount
            );
        }
    }

    public record BulkDeleteResult(
            int requestedCount,
            int deletedCount,
            int missingCount
    ) {
    }

    private void validateProductReference(Long productNo) {
        if (productNo == null) {
            return;
        }
        if (!productRepository.existsById(productNo)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}
