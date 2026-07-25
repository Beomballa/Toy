CREATE TABLE IF NOT EXISTS front_cart (
    cart_no BIGINT NOT NULL AUTO_INCREMENT,
    cart_token VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    crt_dtm DATETIME(6) NULL,
    crt_no BIGINT NULL,
    upt_dtm DATETIME(6) NULL,
    upt_no BIGINT NULL,
    PRIMARY KEY (cart_no),
    UNIQUE KEY uk_front_cart_token (cart_token),
    KEY ix_front_cart_status_updated (status, upt_dtm)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS front_cart_item (
    cart_item_no BIGINT NOT NULL AUTO_INCREMENT,
    cart_no BIGINT NOT NULL,
    product_no BIGINT NOT NULL,
    option_no BIGINT NOT NULL,
    quantity INT NOT NULL,
    crt_dtm DATETIME(6) NULL,
    crt_no BIGINT NULL,
    upt_dtm DATETIME(6) NULL,
    upt_no BIGINT NULL,
    PRIMARY KEY (cart_item_no),
    UNIQUE KEY uk_front_cart_item_option (cart_no, product_no, option_no),
    KEY ix_front_cart_item_cart (cart_no),
    CONSTRAINT fk_front_cart_item_cart FOREIGN KEY (cart_no) REFERENCES front_cart (cart_no),
    CONSTRAINT fk_front_cart_item_product FOREIGN KEY (product_no) REFERENCES product (product_no),
    CONSTRAINT fk_front_cart_item_option FOREIGN KEY (option_no) REFERENCES product_option (option_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_delivery (
    order_delivery_no BIGINT NOT NULL AUTO_INCREMENT,
    order_no BIGINT NOT NULL,
    recipient_name VARCHAR(50) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    address1 VARCHAR(200) NOT NULL,
    address2 VARCHAR(200) NULL,
    delivery_request VARCHAR(200) NULL,
    crt_dtm DATETIME(6) NULL,
    crt_no BIGINT NULL,
    upt_dtm DATETIME(6) NULL,
    upt_no BIGINT NULL,
    PRIMARY KEY (order_delivery_no),
    UNIQUE KEY uk_order_delivery_order (order_no),
    CONSTRAINT fk_order_delivery_order FOREIGN KEY (order_no) REFERENCES orders (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
