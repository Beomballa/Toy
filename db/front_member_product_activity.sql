CREATE TABLE IF NOT EXISTS front_member_product_activity (
    activity_no BIGINT NOT NULL AUTO_INCREMENT,
    member_no BIGINT NOT NULL,
    activity_type VARCHAR(20) NOT NULL,
    product_no BIGINT NOT NULL,
    last_interacted_at DATETIME(6) NOT NULL,
    crt_dtm DATETIME(6) NULL,
    crt_no BIGINT NULL,
    upt_dtm DATETIME(6) NULL,
    upt_no BIGINT NULL,
    PRIMARY KEY (activity_no),
    CONSTRAINT uk_front_member_activity UNIQUE (member_no, activity_type, product_no),
    CONSTRAINT fk_front_member_activity_member FOREIGN KEY (member_no) REFERENCES sy_account (ID),
    CONSTRAINT fk_front_member_activity_product FOREIGN KEY (product_no) REFERENCES product (product_no),
    KEY ix_front_member_activity_recent (member_no, activity_type, last_interacted_at DESC, activity_no DESC),
    KEY ix_front_member_activity_product (product_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
