ALTER TABLE orders
    ADD COLUMN admin_memo VARCHAR(1000) NULL AFTER tracking_num;

CREATE TABLE order_status_history (
    history_no BIGINT NOT NULL AUTO_INCREMENT,
    order_no BIGINT NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    before_status VARCHAR(20) NULL,
    after_status VARCHAR(20) NULL,
    reason VARCHAR(200) NULL,
    admin_memo_snapshot VARCHAR(1000) NULL,
    delivery_company VARCHAR(50) NULL,
    tracking_num VARCHAR(50) NULL,
    crt_dtm DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    crt_no BIGINT NULL,
    upt_dtm DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    upt_no BIGINT NULL,
    PRIMARY KEY (history_no),
    KEY idx_order_status_history_order_no (order_no),
    KEY idx_order_status_history_crt_dtm (crt_dtm)
);
