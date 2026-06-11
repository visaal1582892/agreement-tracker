-- Drop legacy agreement_number column if it still exists (JPA update does not drop columns)
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'agreements'
      AND COLUMN_NAME = 'agreement_number'
);

SET @drop_col = IF(
    @col_exists > 0,
    'ALTER TABLE agreements DROP COLUMN agreement_number',
    'SELECT 1'
);
PREPARE stmt_drop_agreement_number FROM @drop_col;
EXECUTE stmt_drop_agreement_number;
DEALLOCATE PREPARE stmt_drop_agreement_number;
