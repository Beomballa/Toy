CREATE TABLE IF NOT EXISTS front_product_review_report (
    review_report_no BIGINT NOT NULL AUTO_INCREMENT,
    review_no BIGINT NOT NULL,
    member_no BIGINT NOT NULL,
    reason VARCHAR(30) NOT NULL,
    detail VARCHAR(500) NULL,
    crt_dtm DATETIME(6) NULL, crt_no BIGINT NULL, upt_dtm DATETIME(6) NULL, upt_no BIGINT NULL,
    PRIMARY KEY (review_report_no),
    UNIQUE KEY uk_front_product_review_report_member_review (member_no, review_no),
    KEY ix_front_product_review_report_review_created (review_no, review_report_no),
    CONSTRAINT fk_front_product_review_report_review FOREIGN KEY (review_no) REFERENCES front_product_review (review_no),
    CONSTRAINT fk_front_product_review_report_member FOREIGN KEY (member_no) REFERENCES sy_account (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
