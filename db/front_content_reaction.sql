CREATE TABLE IF NOT EXISTS front_content_reaction (
    reaction_no BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_no BIGINT NOT NULL,
    visitor_key VARCHAR(64) NOT NULL,
    reaction_type VARCHAR(20) NOT NULL,
    created_dtm DATETIME NOT NULL,
    updated_dtm DATETIME NOT NULL,
    UNIQUE KEY uk_front_content_reaction_visitor (document_no, visitor_key),
    INDEX idx_front_content_reaction_document_type (document_no, reaction_type),
    INDEX idx_front_content_reaction_updated (updated_dtm)
);

-- Public demo documents receive deterministic reactions without modifying document data.
INSERT IGNORE INTO front_content_reaction
    (document_no, visitor_key, reaction_type, created_dtm, updated_dtm)
SELECT NO, '10000000-0000-4000-8000-000000000001', 'HELPFUL', NOW(), NOW()
FROM CT_DOCUMENT
WHERE status = 'PUBLISHED' AND public_yn = 'Y'
ORDER BY NO DESC
LIMIT 1;

INSERT IGNORE INTO front_content_reaction
    (document_no, visitor_key, reaction_type, created_dtm, updated_dtm)
SELECT NO, '10000000-0000-4000-8000-000000000002', 'HELPFUL', NOW(), NOW()
FROM CT_DOCUMENT
WHERE status = 'PUBLISHED' AND public_yn = 'Y'
ORDER BY NO DESC
LIMIT 1;

INSERT IGNORE INTO front_content_reaction
    (document_no, visitor_key, reaction_type, created_dtm, updated_dtm)
SELECT NO, '10000000-0000-4000-8000-000000000003', 'NOT_HELPFUL', NOW(), NOW()
FROM CT_DOCUMENT
WHERE status = 'PUBLISHED' AND public_yn = 'Y'
ORDER BY NO DESC
LIMIT 1;
