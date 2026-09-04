CREATE TABLE IF NOT EXISTS front_order_claim (
    order_claim_no BIGINT NOT NULL AUTO_INCREMENT,
    order_no BIGINT NOT NULL,
    member_no BIGINT NOT NULL,
    claim_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6) NULL,
    crt_dtm DATETIME(6) NULL, crt_no BIGINT NULL, upt_dtm DATETIME(6) NULL, upt_no BIGINT NULL,
    PRIMARY KEY (order_claim_no),
    KEY ix_front_order_claim_member_created (member_no, order_claim_no),
    KEY ix_front_order_claim_order_status (order_no, status),
    CONSTRAINT fk_front_order_claim_order FOREIGN KEY (order_no) REFERENCES orders (order_no),
    CONSTRAINT fk_front_order_claim_member FOREIGN KEY (member_no) REFERENCES sy_account (ID),
    CONSTRAINT ck_front_order_claim_type CHECK (claim_type IN ('RETURN', 'EXCHANGE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS front_order_claim_history (
    order_claim_history_no BIGINT NOT NULL AUTO_INCREMENT,
    order_claim_no BIGINT NOT NULL,
    before_status VARCHAR(20) NULL,
    after_status VARCHAR(20) NOT NULL,
    memo VARCHAR(1000) NULL,
    crt_dtm DATETIME(6) NULL, crt_no BIGINT NULL, upt_dtm DATETIME(6) NULL, upt_no BIGINT NULL,
    PRIMARY KEY (order_claim_history_no),
    KEY ix_front_order_claim_history_claim (order_claim_no, order_claim_history_no),
    CONSTRAINT fk_front_order_claim_history_claim FOREIGN KEY (order_claim_no) REFERENCES front_order_claim (order_claim_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
