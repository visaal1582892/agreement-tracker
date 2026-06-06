-- Dev DDL (run manually). Flyway disabled until production.
-- Allow NULL on agreement fields not yet filled during draft save.
ALTER TABLE agreements
    MODIFY COLUMN start_date DATE NULL,
    MODIFY COLUMN expiry_date DATE NULL;

ALTER TABLE agreements
    MODIFY COLUMN commercial_structure ENUM('FLAT', 'SLAB') NULL,
    MODIFY COLUMN income_type_id BIGINT NULL,
    MODIFY COLUMN agreement_type_id BIGINT NULL;
