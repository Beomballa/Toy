CREATE TABLE IF NOT EXISTS front_product_review (
    review_no BIGINT NOT NULL AUTO_INCREMENT,
    member_no BIGINT NOT NULL,
    product_no BIGINT NOT NULL,
    order_no BIGINT NOT NULL,
    reviewer_name VARCHAR(40) NOT NULL,
    rating TINYINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    crt_dtm DATETIME(6) NULL, crt_no BIGINT NULL, upt_dtm DATETIME(6) NULL, upt_no BIGINT NULL,
    PRIMARY KEY (review_no),
    UNIQUE KEY uk_front_product_review_member_order_product (member_no, order_no, product_no),
    KEY ix_front_product_review_product_created (product_no, review_no),
    CONSTRAINT fk_front_product_review_member FOREIGN KEY (member_no) REFERENCES sy_account (ID),
    CONSTRAINT fk_front_product_review_product FOREIGN KEY (product_no) REFERENCES product (product_no),
    CONSTRAINT fk_front_product_review_order FOREIGN KEY (order_no) REFERENCES orders (order_no),
    CONSTRAINT ck_front_product_review_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
