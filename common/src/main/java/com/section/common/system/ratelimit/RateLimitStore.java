package com.section.common.system.ratelimit;

import java.time.Duration;
import java.time.Instant;

public interface RateLimitStore {

    boolean isBlocked(String key, Instant now);

    boolean recordFailure(String key, int failureLimit, Duration window, Instant now);

    void clear(String key);

    long trackedCount();
}
