package com.section.admin.content.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class DocumentRepositorySearchIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("콘텐츠 목록은 고정글 우선 정렬과 상태/공개 필터를 함께 적용한다")
    void getDocumentListAppliesPinnedSortingAndVisibilityFilters() {
        Document pinnedNotice = new Document();
        pinnedNotice.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.Y,
                "고정 공지",
                "고정 공지 본문",
                null
        );
        documentRepository.save(pinnedNotice);

        Document hiddenNotice = new Document();
        hiddenNotice.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.N,
                YN.N,
                "비공개 공지",
                "비공개 본문",
                null
        );
        documentRepository.save(hiddenNotice);

        Document draftNotice = new Document();
        draftNotice.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.DRAFT,
                YN.Y,
                YN.N,
                "임시 공지",
                "임시 본문",
                null
        );
        documentRepository.save(draftNotice);

        Page<DocumentListItemDto> pinnedOnly = documentRepository.getDocumentList(
                new DocumentListQuery(Document.BoardType.NOTICE, "공지", Document.PublishStatus.PUBLISHED, YN.Y, true, null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1, pinnedOnly.getTotalElements());
        assertEquals("고정 공지", pinnedOnly.getContent().getFirst().getTitle());
        assertEquals("Y", pinnedOnly.getContent().getFirst().getPinnedYn());
    }

    @Test
    @DisplayName("콘텐츠 목록은 생성일 범위 필터를 함께 적용한다")
    void getDocumentListAppliesCreatedDateRange() {
        Document recentNotice = new Document();
        recentNotice.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.N,
                "이번 주 공지",
                "이번 주 본문",
                null
        );
        documentRepository.save(recentNotice);
        entityManager.createNativeQuery("update CT_DOCUMENT set crt_dtm = :crtDtm where NO = :id")
                .setParameter("crtDtm", java.time.LocalDateTime.of(2026, 5, 20, 12, 0))
                .setParameter("id", recentNotice.getId())
                .executeUpdate();

        Document oldNotice = new Document();
        oldNotice.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.N,
                "지난달 공지",
                "지난달 본문",
                null
        );
        documentRepository.save(oldNotice);
        entityManager.createNativeQuery("update CT_DOCUMENT set crt_dtm = :crtDtm where NO = :id")
                .setParameter("crtDtm", java.time.LocalDateTime.of(2026, 4, 10, 12, 0))
                .setParameter("id", oldNotice.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        Page<DocumentListItemDto> result = documentRepository.getDocumentList(
                new DocumentListQuery(
                        Document.BoardType.NOTICE,
                        null,
                        Document.PublishStatus.PUBLISHED,
                        YN.Y,
                        false,
                        java.time.LocalDateTime.of(2026, 5, 1, 0, 0),
                        java.time.LocalDateTime.of(2026, 5, 31, 23, 59, 59)
                ),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("이번 주 공지", result.getContent().getFirst().getTitle());
    }
}
