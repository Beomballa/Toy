CREATE TABLE IF NOT EXISTS front_product_display (
    display_no BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_no BIGINT NOT NULL,
    headline VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    mood VARCHAR(120) NOT NULL,
    featured_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    featured_rank INT NOT NULL DEFAULT 999,
    crt_dtm DATETIME NULL,
    crt_no BIGINT NULL,
    upt_dtm DATETIME NULL,
    upt_no BIGINT NULL,
    INDEX idx_front_product_display_featured_rank (featured_yn, featured_rank),
    CONSTRAINT uk_front_product_display_product UNIQUE (product_no)
);
