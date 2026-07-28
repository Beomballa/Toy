CREATE TABLE IF NOT EXISTS request_rate_limit_bucket (
    rate_key VARCHAR(160) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    window_started_at DATETIME(6) NOT NULL,
    last_attempt_at DATETIME(6) NOT NULL,
    blocked_until DATETIME(6) NULL,
    PRIMARY KEY (rate_key),
    KEY ix_request_rate_limit_expiry (last_attempt_at, blocked_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
