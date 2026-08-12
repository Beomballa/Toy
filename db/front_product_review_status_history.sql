CREATE TABLE IF NOT EXISTS front_product_review_status_history (
    review_status_history_no BIGINT NOT NULL AUTO_INCREMENT,
    review_no BIGINT NOT NULL,
    before_status VARCHAR(20) NOT NULL,
    after_status VARCHAR(20) NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    crt_dtm DATETIME(6) NULL, crt_no BIGINT NULL, upt_dtm DATETIME(6) NULL, upt_no BIGINT NULL,
    PRIMARY KEY (review_status_history_no),
    KEY ix_front_product_review_status_history_review_created (review_no, review_status_history_no),
    CONSTRAINT fk_front_product_review_status_history_review FOREIGN KEY (review_no) REFERENCES front_product_review (review_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
