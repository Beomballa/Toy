CREATE TABLE IF NOT EXISTS front_content_view_event (
    event_no BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_no BIGINT NOT NULL,
    visitor_key VARCHAR(64) NOT NULL,
    viewed_date DATE NOT NULL,
    viewed_dtm DATETIME NOT NULL,
    UNIQUE KEY uk_front_content_view_daily (document_no, visitor_key, viewed_date),
    INDEX idx_front_content_view_document_dtm (document_no, viewed_dtm),
    INDEX idx_front_content_view_date (viewed_date)
);

-- Historical fixtures keep local analytics screens testable without affecting live visitor de-duplication.
INSERT IGNORE INTO front_content_view_event (document_no, visitor_key, viewed_date, viewed_dtm)
SELECT NO,
       CONCAT('demo-seed-', NO),
       DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY),
       DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM CT_DOCUMENT
WHERE status = 'PUBLISHED'
  AND public_yn = 'Y'
ORDER BY NO
LIMIT 4;
