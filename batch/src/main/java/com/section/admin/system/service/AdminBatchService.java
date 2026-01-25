package com.section.admin.system.service;

import com.section.common.content.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminBatchService {

    private final DocumentRepository documentRepository;

    public ContentRecentListResDto getRecent7DaysDocumentList(LocalDateTime startDt, LocalDateTime endDt) {
        List<Document> recentDocs = documentRepository.getRecent7DaysDocumentList(now.minusDays(7), now);

        return new ContentRecentListResDto(recentDocs);
    }
}
