package com.section.admin.system.schedule;

import com.section.admin.system.service.AdminBatchService;
import com.section.admin.system.service.AdminBatchService.DocumentStatsAggregationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleExecutionTest {

    @Test
    @DisplayName("문서 통계 스케줄은 실제 집계 서비스를 호출한다")
    void invokesDocumentStatsAggregation() {
        AdminBatchService service = mock(AdminBatchService.class);
        when(service.aggregateDocumentStats())
                .thenReturn(new DocumentStatsAggregationResult(LocalDate.of(2026, 7, 21), 8, 5));
        Schedule schedule = new Schedule(service);

        schedule.documentStatsSchedule();

        verify(service).aggregateDocumentStats();
    }
}
