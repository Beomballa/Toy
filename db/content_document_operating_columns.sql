ALTER TABLE ct_document
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' AFTER board_type,
    ADD COLUMN public_yn VARCHAR(1) NOT NULL DEFAULT 'Y' AFTER status,
    ADD COLUMN pinned_yn VARCHAR(1) NOT NULL DEFAULT 'N' AFTER public_yn;

CREATE INDEX idx_ct_document_status ON ct_document (status);
CREATE INDEX idx_ct_document_public_yn ON ct_document (public_yn);
CREATE INDEX idx_ct_document_pinned_yn ON ct_document (pinned_yn);
