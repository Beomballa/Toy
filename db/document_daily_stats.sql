CREATE TABLE IF NOT EXISTS document_daily_stats (
    stats_no BIGINT NOT NULL AUTO_INCREMENT,
    snapshot_date DATE NOT NULL,
    scope VARCHAR(20) NOT NULL,
    total_count BIGINT NOT NULL DEFAULT 0,
    published_count BIGINT NOT NULL DEFAULT 0,
    draft_count BIGINT NOT NULL DEFAULT 0,
    public_count BIGINT NOT NULL DEFAULT 0,
    private_count BIGINT NOT NULL DEFAULT 0,
    pinned_count BIGINT NOT NULL DEFAULT 0,
    linked_count BIGINT NOT NULL DEFAULT 0,
    total_view_count BIGINT NOT NULL DEFAULT 0,
    aggregated_at DATETIME NOT NULL,
    PRIMARY KEY (stats_no),
    CONSTRAINT uk_document_daily_stats_date_scope UNIQUE (snapshot_date, scope),
    KEY idx_document_daily_stats_date (snapshot_date)
);

INSERT INTO document_daily_stats (
    snapshot_date,
    scope,
    total_count,
    published_count,
    draft_count,
    public_count,
    private_count,
    pinned_count,
    linked_count,
    total_view_count,
    aggregated_at
)
SELECT
    CURRENT_DATE,
    board_type,
    COUNT(*),
    SUM(status = 'PUBLISHED'),
    SUM(status = 'DRAFT'),
    SUM(public_yn = 'Y'),
    SUM(public_yn = 'N'),
    SUM(pinned_yn = 'Y'),
    SUM(product_no IS NOT NULL),
    COALESCE(SUM(view_cnt), 0),
    NOW()
FROM CT_DOCUMENT
GROUP BY board_type
ON DUPLICATE KEY UPDATE
    total_count = VALUES(total_count),
    published_count = VALUES(published_count),
    draft_count = VALUES(draft_count),
    public_count = VALUES(public_count),
    private_count = VALUES(private_count),
    pinned_count = VALUES(pinned_count),
    linked_count = VALUES(linked_count),
    total_view_count = VALUES(total_view_count),
    aggregated_at = VALUES(aggregated_at);

INSERT INTO document_daily_stats (
    snapshot_date,
    scope,
    total_count,
    published_count,
    draft_count,
    public_count,
    private_count,
    pinned_count,
    linked_count,
    total_view_count,
    aggregated_at
)
SELECT
    CURRENT_DATE,
    'TOTAL',
    COUNT(*),
    SUM(status = 'PUBLISHED'),
    SUM(status = 'DRAFT'),
    SUM(public_yn = 'Y'),
    SUM(public_yn = 'N'),
    SUM(pinned_yn = 'Y'),
    SUM(product_no IS NOT NULL),
    COALESCE(SUM(view_cnt), 0),
    NOW()
FROM CT_DOCUMENT
ON DUPLICATE KEY UPDATE
    total_count = VALUES(total_count),
    published_count = VALUES(published_count),
    draft_count = VALUES(draft_count),
    public_count = VALUES(public_count),
    private_count = VALUES(private_count),
    pinned_count = VALUES(pinned_count),
    linked_count = VALUES(linked_count),
    total_view_count = VALUES(total_view_count),
    aggregated_at = VALUES(aggregated_at);
