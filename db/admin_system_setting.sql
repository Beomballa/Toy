CREATE TABLE IF NOT EXISTS admin_system_setting (
    setting_no BIGINT NOT NULL AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL,
    setting_value VARCHAR(500) NOT NULL,
    description VARCHAR(500) NULL,
    crt_dtm DATETIME NULL,
    crt_no BIGINT NULL,
    upt_dtm DATETIME NULL,
    upt_no BIGINT NULL,
    PRIMARY KEY (setting_no),
    UNIQUE KEY uk_admin_system_setting_key (setting_key)
);
