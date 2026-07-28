package com.section.common.system.ratelimit;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RateLimitBucketRepository extends JpaRepository<RateLimitBucket, String> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO request_rate_limit_bucket
                (rate_key, attempt_count, window_started_at, last_attempt_at, blocked_until)
            VALUES (:rateKey, 0, :now, :now, NULL)
            """, nativeQuery = true)
    int insertIfAbsent(@Param("rateKey") String rateKey, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bucket FROM RateLimitBucket bucket WHERE bucket.key = :rateKey")
    Optional<RateLimitBucket> findByKeyForUpdate(@Param("rateKey") String rateKey);

    @Modifying
    @Query("""
            DELETE FROM RateLimitBucket bucket
             WHERE bucket.lastAttemptAt < :expiredBefore
               AND (bucket.blockedUntil IS NULL OR bucket.blockedUntil < :expiredBefore)
            """)
    int deleteExpired(@Param("expiredBefore") LocalDateTime expiredBefore);
}
