-- Allow multiple agreements per company agreement group (and state/product scoping is not unique at DB level).
SET @uk_agreement_group_type = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'agreements'
      AND CONSTRAINT_NAME = 'uk_agreement_group_type'
    LIMIT 1
);

SET @drop_uk_agreement_group_type = IF(
    @uk_agreement_group_type IS NOT NULL,
    'ALTER TABLE agreements DROP INDEX uk_agreement_group_type',
    'SELECT 1'
);

PREPARE stmt_drop_uk_agreement_group_type FROM @drop_uk_agreement_group_type;
EXECUTE stmt_drop_uk_agreement_group_type;
DEALLOCATE PREPARE stmt_drop_uk_agreement_group_type;
