CREATE TABLE IF NOT EXISTS front_member_delivery_address (
    address_no BIGINT NOT NULL AUTO_INCREMENT,
    member_no BIGINT NOT NULL,
    address_name VARCHAR(40) NOT NULL,
    recipient_name VARCHAR(50) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    address1 VARCHAR(200) NOT NULL,
    address2 VARCHAR(200) NULL,
    default_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    crt_dtm DATETIME(6) NULL, crt_no BIGINT NULL, upt_dtm DATETIME(6) NULL, upt_no BIGINT NULL,
    PRIMARY KEY (address_no),
    KEY ix_front_member_delivery_address_member (member_no, default_yn, address_no),
    CONSTRAINT fk_front_member_delivery_address_member FOREIGN KEY (member_no) REFERENCES sy_account (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
