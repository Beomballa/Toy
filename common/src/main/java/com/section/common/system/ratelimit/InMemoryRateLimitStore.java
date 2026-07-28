package com.section.common.system.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimitStore implements RateLimitStore {
    private final Map<String, TestBucket> buckets = new ConcurrentHashMap<>();
    private final int maximumSize;

    public InMemoryRateLimitStore() {
        this(Integer.MAX_VALUE);
    }

    public InMemoryRateLimitStore(int maximumSize) {
        this.maximumSize = Math.max(1, maximumSize);
    }

    @Override
    public boolean isBlocked(String key, Instant now) {
        TestBucket bucket = buckets.get(key);
        LocalDateTime current = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        return bucket != null && bucket.blockedUntil != null && bucket.blockedUntil.isAfter(current);
    }

    @Override
    public boolean recordFailure(String key, int failureLimit, Duration window, Instant now) {
        LocalDateTime current = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        TestBucket bucket = buckets.compute(key, (ignored, previous) -> {
            TestBucket target = previous == null ? new TestBucket(current) : previous;
            target.record(current, Math.max(1, failureLimit), window);
            return target;
        });
        evictOldest();
        return bucket.blockedUntil != null && bucket.blockedUntil.isAfter(current);
    }

    @Override
    public void clear(String key) {
        buckets.remove(key);
    }

    @Override
    public long trackedCount() {
        return buckets.size();
    }

    private void evictOldest() {
        while (buckets.size() > maximumSize) {
            buckets.entrySet().stream()
                    .min(Map.Entry.comparingByValue((left, right) -> left.lastAttemptAt.compareTo(right.lastAttemptAt)))
                    .ifPresent(entry -> buckets.remove(entry.getKey(), entry.getValue()));
        }
    }

    private static final class TestBucket {
        private int attempts;
        private LocalDateTime lastAttemptAt;
        private LocalDateTime blockedUntil;

        private TestBucket(LocalDateTime now) {
            this.lastAttemptAt = now;
        }

        private void record(LocalDateTime now, int failureLimit, Duration window) {
            if (!lastAttemptAt.plus(window).isAfter(now) || blockedUntil != null && !blockedUntil.isAfter(now)) {
                attempts = 0;
                blockedUntil = null;
            }
            attempts++;
            lastAttemptAt = now;
            if (attempts >= failureLimit) {
                blockedUntil = now.plus(window);
            }
        }
    }
}
