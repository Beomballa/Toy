package com.section.admin.content.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
                new DocumentListQuery(Document.BoardType.NOTICE, "공지", Document.PublishStatus.PUBLISHED, YN.Y, true),
                PageRequest.of(0, 10)
        );

        assertEquals(1, pinnedOnly.getTotalElements());
        assertEquals("고정 공지", pinnedOnly.getContent().getFirst().getTitle());
        assertEquals("Y", pinnedOnly.getContent().getFirst().getPinnedYn());
    }
}
