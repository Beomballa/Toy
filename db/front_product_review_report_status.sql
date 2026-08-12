SET @review_report_status_column_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'front_product_review_report' AND column_name = 'status'
);
SET @review_report_status_column_sql := IF(
    @review_report_status_column_exists = 0,
    'ALTER TABLE front_product_review_report ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' AFTER detail',
    'SELECT 1'
);
PREPARE review_report_status_column_statement FROM @review_report_status_column_sql;
EXECUTE review_report_status_column_statement;
DEALLOCATE PREPARE review_report_status_column_statement;

SET @review_report_status_index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'front_product_review_report'
      AND index_name = 'ix_front_product_review_report_status_review'
);
SET @review_report_status_index_sql := IF(
    @review_report_status_index_exists = 0,
    'ALTER TABLE front_product_review_report ADD KEY ix_front_product_review_report_status_review (status, review_no)',
    'SELECT 1'
);
PREPARE review_report_status_index_statement FROM @review_report_status_index_sql;
EXECUTE review_report_status_index_statement;
DEALLOCATE PREPARE review_report_status_index_statement;
