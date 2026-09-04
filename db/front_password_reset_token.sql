CREATE TABLE IF NOT EXISTS front_password_reset_token (
    password_reset_token_no BIGINT NOT NULL AUTO_INCREMENT,
    member_no BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    crt_dtm DATETIME(6) NULL, crt_no BIGINT NULL, upt_dtm DATETIME(6) NULL, upt_no BIGINT NULL,
    PRIMARY KEY (password_reset_token_no),
    UNIQUE KEY uk_front_password_reset_token_hash (token_hash),
    KEY ix_front_password_reset_member_expiry (member_no, expires_at),
    CONSTRAINT fk_front_password_reset_member FOREIGN KEY (member_no) REFERENCES sy_account (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
