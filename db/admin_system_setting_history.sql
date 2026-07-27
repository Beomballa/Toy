CREATE TABLE IF NOT EXISTS admin_system_setting_history (
    history_no BIGINT NOT NULL AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL,
    setting_name VARCHAR(100) NOT NULL,
    before_value VARCHAR(500) NULL,
    after_value VARCHAR(500) NOT NULL,
    change_summary VARCHAR(500) NOT NULL,
    changed_ip_address VARCHAR(50) NOT NULL,
    crt_dtm DATETIME NULL,
    crt_no BIGINT NULL,
    upt_dtm DATETIME NULL,
    upt_no BIGINT NULL,
    PRIMARY KEY (history_no),
    KEY idx_admin_system_setting_history_key_dtm (setting_key, crt_dtm),
    KEY idx_admin_system_setting_history_admin_dtm (crt_no, crt_dtm)
);
