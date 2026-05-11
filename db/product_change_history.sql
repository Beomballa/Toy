CREATE TABLE IF NOT EXISTS product_change_history (
    history_no BIGINT NOT NULL AUTO_INCREMENT,
    product_no BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    status_snapshot VARCHAR(20) NULL,
    option_count INT NOT NULL DEFAULT 0,
    total_stock BIGINT NOT NULL DEFAULT 0,
    crt_dtm DATETIME NULL,
    crt_no BIGINT NULL,
    upt_dtm DATETIME NULL,
    upt_no BIGINT NULL,
    PRIMARY KEY (history_no),
    KEY idx_product_change_history_product_no_history_no (product_no, history_no)
);
