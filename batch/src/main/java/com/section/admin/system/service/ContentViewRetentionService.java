package com.section.admin.system.service;

import com.section.common.content.repository.FrontContentViewEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@Transactional
public class ContentViewRetentionService {

    static final int MIN_RETENTION_DAYS = 30;
    static final int MAX_RETENTION_DAYS = 3650;

    private final FrontContentViewEventRepository viewEventRepository;
    private final Clock clock;

    @Autowired
    public ContentViewRetentionService(FrontContentViewEventRepository viewEventRepository) {
        this(viewEventRepository, Clock.systemDefaultZone());
    }

    ContentViewRetentionService(FrontContentViewEventRepository viewEventRepository, Clock clock) {
        this.viewEventRepository = viewEventRepository;
        this.clock = clock;
    }

    public RetentionResult purgeExpiredEvents(int retentionDays) {
        validateRetentionDays(retentionDays);
        LocalDate retentionStartDate = LocalDate.now(clock).minusDays(retentionDays - 1L);
        int deletedCount = viewEventRepository.deleteBefore(retentionStartDate);
        return new RetentionResult(retentionStartDate, retentionDays, deletedCount);
    }

    private void validateRetentionDays(int retentionDays) {
        if (retentionDays < MIN_RETENTION_DAYS || retentionDays > MAX_RETENTION_DAYS) {
            throw new IllegalArgumentException(
                    "Content view retention days must be between "
                            + MIN_RETENTION_DAYS + " and " + MAX_RETENTION_DAYS
            );
        }
    }

    public record RetentionResult(
            LocalDate retentionStartDate,
            int retentionDays,
            int deletedCount
    ) {
    }
}
