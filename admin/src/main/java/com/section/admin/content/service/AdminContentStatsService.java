package com.section.admin.content.service;

import com.section.admin.content.res.ContentDailyStatsResponse;
import com.section.common.content.repository.DocumentDailyStatsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminContentStatsService {

    private final DocumentDailyStatsSnapshotRepository snapshotRepository;

    public ContentDailyStatsResponse getLatestStats() {
        return snapshotRepository.findTopByOrderBySnapshotDateDesc()
                .map(latest -> ContentDailyStatsResponse.from(
                        snapshotRepository.findAllBySnapshotDateOrderByScopeAsc(latest.getSnapshotDate())
                ))
                .orElseGet(ContentDailyStatsResponse::empty);
    }
}
