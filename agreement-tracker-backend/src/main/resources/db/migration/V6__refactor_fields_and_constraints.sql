-- Move agreement_type_id to agreements, add group owner, drop agreement_number, add uniqueness

ALTER TABLE agreements
    ADD COLUMN agreement_type_id BIGINT NULL,
    ADD CONSTRAINT fk_agreements_agreement_type
        FOREIGN KEY (agreement_type_id) REFERENCES agreement_types (id);

UPDATE agreements a
    INNER JOIN (
        SELECT av.agreement_id, av.agreement_type_id
        FROM agreement_versions av
        INNER JOIN (
            SELECT agreement_id, MIN(version_number) AS min_ver
            FROM agreement_versions
            WHERE agreement_type_id IS NOT NULL
            GROUP BY agreement_id
        ) first_ver
            ON first_ver.agreement_id = av.agreement_id
           AND first_ver.min_ver = av.version_number
        WHERE av.agreement_type_id IS NOT NULL
    ) src ON src.agreement_id = a.id
SET a.agreement_type_id = src.agreement_type_id;

ALTER TABLE agreements
    MODIFY COLUMN agreement_name VARCHAR(255) NULL;

SET @ag_number_idx = (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'agreements'
      AND COLUMN_NAME = 'agreement_number'
      AND NON_UNIQUE = 0
    LIMIT 1
);
SET @drop_ag_number_idx = IF(
    @ag_number_idx IS NOT NULL,
    CONCAT('ALTER TABLE agreements DROP INDEX `', @ag_number_idx, '`'),
    'SELECT 1'
);
PREPARE stmt_drop_ag_number_idx FROM @drop_ag_number_idx;
EXECUTE stmt_drop_ag_number_idx;
DEALLOCATE PREPARE stmt_drop_ag_number_idx;

SET @ag_number_nonuniq_idx = (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'agreements'
      AND INDEX_NAME = 'idx_ag_agreement_number'
    LIMIT 1
);
SET @drop_ag_number_nonuniq = IF(
    @ag_number_nonuniq_idx IS NOT NULL,
    'ALTER TABLE agreements DROP INDEX idx_ag_agreement_number',
    'SELECT 1'
);
PREPARE stmt_drop_ag_number_nonuniq FROM @drop_ag_number_nonuniq;
EXECUTE stmt_drop_ag_number_nonuniq;
DEALLOCATE PREPARE stmt_drop_ag_number_nonuniq;

ALTER TABLE agreements
    DROP COLUMN agreement_number;

ALTER TABLE agreements
    ADD CONSTRAINT uk_agreement_group_type
        UNIQUE (company_agreement_group_id, agreement_type_id);

SET @av_type_fk = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'agreement_versions'
      AND COLUMN_NAME = 'agreement_type_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);
SET @drop_av_type_fk = IF(
    @av_type_fk IS NOT NULL,
    CONCAT('ALTER TABLE agreement_versions DROP FOREIGN KEY `', @av_type_fk, '`'),
    'SELECT 1'
);
PREPARE stmt_drop_av_type_fk FROM @drop_av_type_fk;
EXECUTE stmt_drop_av_type_fk;
DEALLOCATE PREPARE stmt_drop_av_type_fk;

ALTER TABLE agreement_versions
    DROP COLUMN agreement_type_id;

ALTER TABLE company_agreement_groups
    ADD COLUMN owner_user_id BIGINT NULL;

UPDATE company_agreement_groups
SET owner_user_id = created_by_user_id
WHERE owner_user_id IS NULL;

ALTER TABLE company_agreement_groups
    MODIFY COLUMN owner_user_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_cag_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES users (id);

CREATE INDEX idx_cag_owner_user_id ON company_agreement_groups (owner_user_id);
