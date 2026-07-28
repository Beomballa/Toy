package com.section.common.system.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class PersistentRateLimitStore implements RateLimitStore {
    private static final long CLEANUP_INTERVAL = 256;
    private static final Duration RETENTION = Duration.ofDays(1);

    private final RateLimitBucketRepository repository;
    private final AtomicLong writeCount = new AtomicLong();

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(String key, Instant now) {
        LocalDateTime current = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        return repository.findById(key).map(bucket -> bucket.isBlocked(current)).orElse(false);
    }

    @Override
    @Transactional
    public boolean recordFailure(String key, int failureLimit, Duration window, Instant now) {
        LocalDateTime current = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        repository.insertIfAbsent(key, current);
        RateLimitBucket bucket = repository.findByKeyForUpdate(key)
                .orElseThrow(() -> new IllegalStateException("요청 제한 버킷을 생성하지 못했습니다."));
        boolean blocked = bucket.recordFailure(current, Math.max(1, failureLimit), window);
        if (writeCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            repository.deleteExpired(current.minus(RETENTION));
        }
        return blocked;
    }

    @Override
    @Transactional
    public void clear(String key) {
        repository.deleteById(key);
    }

    @Override
    @Transactional(readOnly = true)
    public long trackedCount() {
        return repository.count();
    }
}
