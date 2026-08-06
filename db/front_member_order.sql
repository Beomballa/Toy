SET @member_no_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'member_no'
);
SET @member_no_column_sql := IF(
    @member_no_column_exists = 0,
    'ALTER TABLE orders ADD COLUMN member_no BIGINT NULL AFTER buyer_phone',
    'SELECT 1'
);
PREPARE member_no_column_statement FROM @member_no_column_sql;
EXECUTE member_no_column_statement;
DEALLOCATE PREPARE member_no_column_statement;

SET @member_order_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'idx_orders_member_created'
);
SET @member_order_index_sql := IF(
    @member_order_index_exists = 0,
    'ALTER TABLE orders ADD KEY idx_orders_member_created (member_no, crt_dtm)',
    'SELECT 1'
);
PREPARE member_order_index_statement FROM @member_order_index_sql;
EXECUTE member_order_index_statement;
DEALLOCATE PREPARE member_order_index_statement;
