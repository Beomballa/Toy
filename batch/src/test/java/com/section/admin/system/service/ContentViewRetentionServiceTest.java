package com.section.admin.system.service;

import com.section.common.content.repository.FrontContentViewEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentViewRetentionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-23T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final FrontContentViewEventRepository repository = mock(FrontContentViewEventRepository.class);
    private final ContentViewRetentionService service =
            new ContentViewRetentionService(repository, FIXED_CLOCK);

    @Test
    @DisplayName("조회 이벤트 보존 정책은 오늘을 포함한 설정 일수보다 오래된 데이터만 삭제한다")
    void purgesEventsBeforeRetentionStartDate() {
        LocalDate retentionStartDate = LocalDate.of(2026, 6, 24);
        when(repository.deleteOrphanEvents()).thenReturn(3);
        when(repository.deleteBefore(retentionStartDate)).thenReturn(37);

        ContentViewRetentionService.RetentionResult result = service.purgeExpiredEvents(30);

        assertThat(result.retentionStartDate()).isEqualTo(retentionStartDate);
        assertThat(result.retentionDays()).isEqualTo(30);
        assertThat(result.orphanDeletedCount()).isEqualTo(3);
        assertThat(result.expiredDeletedCount()).isEqualTo(37);
        assertThat(result.totalDeletedCount()).isEqualTo(40);
        verify(repository).deleteOrphanEvents();
        verify(repository).deleteBefore(retentionStartDate);
    }

    @Test
    @DisplayName("조회 이벤트 보존 일수는 안전 범위를 벗어나면 삭제를 실행하지 않는다")
    void rejectsUnsafeRetentionDays() {
        assertThatThrownBy(() -> service.purgeExpiredEvents(29))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 30 and 3650");
        assertThatThrownBy(() -> service.purgeExpiredEvents(3651))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).deleteBefore(org.mockito.ArgumentMatchers.any());
        verify(repository, never()).deleteOrphanEvents();
    }
}
