package com.section.admin.content.service;

import com.section.common.base.entity.type.YN;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.content.service.DocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    @DisplayName("게시글 수정은 기존 엔티티를 유지한 채 변경 필드만 반영한다")
    void saveDocumentUpdatesExistingEntityWithDirtyChecking() {
        Document existing = new Document();
        existing.setId(1L);
        existing.setViewCnt(33);
        existing.setBoardType(Document.BoardType.NOTICE);
        existing.setStatus(Document.PublishStatus.DRAFT);
        existing.setPublicYn(YN.Y);
        existing.setPinnedYn(YN.N);
        existing.setTitle("기존 제목");
        existing.setContent("기존 내용");

        Document editorInput = new Document();
        editorInput.setId(1L);
        editorInput.applyEditorValues(
                Document.BoardType.DISCUSS,
                Document.PublishStatus.PUBLISHED,
                YN.N,
                YN.Y,
                "새 제목",
                "새 내용",
                44L
        );

        when(documentRepository.findById(1L)).thenReturn(Optional.of(existing));

        Document saved = documentService.saveDocument(editorInput);

        assertEquals(existing, saved);
        assertEquals(33, existing.getViewCnt());
        assertEquals(Document.BoardType.DISCUSS, existing.getBoardType());
        assertEquals(Document.PublishStatus.PUBLISHED, existing.getStatus());
        assertEquals(YN.N, existing.getPublicYn());
        assertEquals(YN.Y, existing.getPinnedYn());
        assertEquals("새 제목", existing.getTitle());
        assertEquals("새 내용", existing.getContent());
        assertEquals(44L, existing.getProductNo());
        verify(documentRepository, never()).save(editorInput);
    }

    @Test
    @DisplayName("커뮤니티 일괄 운영은 기존 엔티티를 유지한 채 상태/공개/고정 값만 반영한다")
    void bulkOperateDocumentsUpdatesExistingEntityWithDirtyChecking() {
        Document first = new Document();
        first.setId(1L);
        first.setStatus(Document.PublishStatus.DRAFT);
        first.setPublicYn(YN.Y);
        first.setPinnedYn(YN.N);

        Document second = new Document();
        second.setId(2L);
        second.setStatus(Document.PublishStatus.DRAFT);
        second.setPublicYn(YN.Y);
        second.setPinnedYn(YN.N);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(first));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(second));

        int updatedCount = documentService.bulkOperateDocuments(
                Set.of(1L, 2L),
                Document.PublishStatus.PUBLISHED,
                YN.N,
                YN.Y
        );

        assertEquals(2, updatedCount);
        assertEquals(Document.PublishStatus.PUBLISHED, first.getStatus());
        assertEquals(YN.N, first.getPublicYn());
        assertEquals(YN.Y, first.getPinnedYn());
        assertEquals(Document.PublishStatus.PUBLISHED, second.getStatus());
        assertEquals(YN.N, second.getPublicYn());
        assertEquals(YN.Y, second.getPinnedYn());
    }
}
