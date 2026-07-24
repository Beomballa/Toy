ALTER TABLE admin_operation_task
    ADD COLUMN source_type VARCHAR(30) NULL AFTER is_pinned,
    ADD COLUMN source_id BIGINT NULL AFTER source_type,
    ADD CONSTRAINT uk_admin_operation_task_source UNIQUE (source_type, source_id);
