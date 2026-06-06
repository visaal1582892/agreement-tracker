-- Dev DDL (run manually). Flyway disabled until production.
-- Draft groups may exist without a company; completeness enforced in Spring on submit.
ALTER TABLE agreement_groups
    MODIFY COLUMN company_id BIGINT NULL;

ALTER TABLE agreements
    ADD COLUMN agreement_name VARCHAR(255) NULL;

UPDATE agreements a
    INNER JOIN agreement_groups g ON a.agreement_group_id = g.id
SET a.agreement_name = g.agreement_number
WHERE a.agreement_name IS NULL;

ALTER TABLE agreements
    MODIFY COLUMN agreement_name VARCHAR(255) NOT NULL;
