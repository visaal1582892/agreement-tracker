-- Structural refactor: introduce company_agreement_groups hierarchy.
-- Greenfield — no data preservation required.
--
-- Rename sequence (critical):
--   agreements          -> agreement_versions  (was version entity)
--   agreement_groups    -> agreements          (was group entity)
--   NEW                 -> company_agreement_groups

SET FOREIGN_KEY_CHECKS = 0;

-- ── 1. Rename core tables ───────────────────────────────────────────────────
RENAME TABLE agreements TO agreement_versions;
RENAME TABLE agreement_groups TO agreements;

-- ── 2. company_agreement_groups ─────────────────────────────────────────────
CREATE TABLE company_agreement_groups (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    company_id          BIGINT          NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    is_active           TINYINT(1)      NOT NULL DEFAULT 1,
    created_by_user_id  BIGINT          NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by_user_id  BIGINT          NULL,
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_cag_company_name UNIQUE (company_id, name),
    CONSTRAINT fk_cag_company FOREIGN KEY (company_id) REFERENCES company_master (id),
    INDEX idx_cag_company_id (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 3. agreement_versions: parent FK rename, drop version-level name ──────────
ALTER TABLE agreement_versions
    CHANGE COLUMN agreement_group_id agreement_id BIGINT NOT NULL;

ALTER TABLE agreement_versions
    DROP COLUMN agreement_name;

-- ── 4. agreements (ex-groups): name on parent, route company via CAG ─────────
ALTER TABLE agreements
    ADD COLUMN agreement_name VARCHAR(255) NOT NULL DEFAULT '' AFTER agreement_number;

ALTER TABLE agreements
    ADD COLUMN company_agreement_group_id BIGINT NOT NULL AFTER agreement_name;

ALTER TABLE agreements
    DROP COLUMN company_id;

-- ── 5. Child tables: agreement_id -> agreement_version_id ─────────────────────
ALTER TABLE agreement_purchase_slabs
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NOT NULL;

ALTER TABLE agreement_sale_targets
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NOT NULL;

ALTER TABLE agreement_manufacturers
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NOT NULL;

ALTER TABLE agreement_vendors
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NOT NULL;

ALTER TABLE agreement_approvals
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NOT NULL;

ALTER TABLE agreement_reminders
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NOT NULL;

ALTER TABLE agreement_action_requests
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NOT NULL;

ALTER TABLE agreement_product_rules
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NOT NULL;

ALTER TABLE agreement_division_rules
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NOT NULL;

ALTER TABLE agreement_computed_products
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NOT NULL;

ALTER TABLE agreement_documents
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NOT NULL;

-- agreement_audits: rename version FK first, then group FK -> agreement_id
ALTER TABLE agreement_audits
    CHANGE COLUMN agreement_id agreement_version_id BIGINT NULL;

ALTER TABLE agreement_audits
    CHANGE COLUMN agreement_group_id agreement_id BIGINT NULL;

-- ── 6. Drop stale indexes / constraints on renamed columns ──────────────────
ALTER TABLE agreement_versions
    DROP INDEX idx_agr_group_id;

ALTER TABLE agreement_versions
    DROP INDEX uk_group_version;

ALTER TABLE agreements
    DROP INDEX idx_ag_agreement_number;

ALTER TABLE agreements
    DROP INDEX idx_ag_company_id;

ALTER TABLE agreement_purchase_slabs
    DROP INDEX idx_aps_agreement_id;

ALTER TABLE agreement_sale_targets
    DROP INDEX idx_ast_agreement_id,
    DROP INDEX uk_target_coordinate;

ALTER TABLE agreement_manufacturers
    DROP INDEX idx_am_agreement_id;

ALTER TABLE agreement_vendors
    DROP INDEX idx_av_agreement_id;

ALTER TABLE agreement_approvals
    DROP INDEX idx_approval_agreement_id;

ALTER TABLE agreement_reminders
    DROP INDEX idx_reminder_agreement_id;

ALTER TABLE agreement_action_requests
    DROP INDEX idx_action_req_agreement_id;

ALTER TABLE agreement_product_rules
    DROP INDEX idx_apr_agreement_id;

ALTER TABLE agreement_division_rules
    DROP INDEX idx_adr_agreement_id;

ALTER TABLE agreement_computed_products
    DROP INDEX idx_acp_agreement_id;

ALTER TABLE agreement_documents
    DROP INDEX idx_adoc_agreement_id;

ALTER TABLE agreement_audits
    DROP INDEX idx_audit_group_id,
    DROP INDEX idx_audit_agreement_id;

-- ── 7. Recreate indexes ─────────────────────────────────────────────────────
ALTER TABLE agreement_versions
    ADD INDEX idx_av_agreement_id (agreement_id),
    ADD CONSTRAINT uk_agreement_version UNIQUE (agreement_id, version_number);

ALTER TABLE agreements
    ADD INDEX idx_ag_agreement_number (agreement_number),
    ADD INDEX idx_ag_cag_id (company_agreement_group_id);

ALTER TABLE agreement_purchase_slabs
    ADD INDEX idx_aps_agreement_version_id (agreement_version_id);

ALTER TABLE agreement_sale_targets
    ADD INDEX idx_ast_agreement_version_id (agreement_version_id),
    ADD CONSTRAINT uk_target_coordinate UNIQUE (agreement_version_id, time_period_id, slab_id);

ALTER TABLE agreement_manufacturers
    ADD INDEX idx_am_agreement_version_id (agreement_version_id);

ALTER TABLE agreement_vendors
    ADD INDEX idx_av_agreement_version_id (agreement_version_id);

ALTER TABLE agreement_approvals
    ADD INDEX idx_approval_agreement_version_id (agreement_version_id);

ALTER TABLE agreement_reminders
    ADD INDEX idx_reminder_agreement_version_id (agreement_version_id);

ALTER TABLE agreement_action_requests
    ADD INDEX idx_action_req_agreement_version_id (agreement_version_id);

ALTER TABLE agreement_product_rules
    ADD INDEX idx_apr_agreement_version_id (agreement_version_id);

ALTER TABLE agreement_division_rules
    ADD INDEX idx_adr_agreement_version_id (agreement_version_id);

ALTER TABLE agreement_computed_products
    ADD INDEX idx_acp_agreement_version_id (agreement_version_id);

ALTER TABLE agreement_documents
    ADD INDEX idx_adoc_agreement_version_id (agreement_version_id);

ALTER TABLE agreement_audits
    ADD INDEX idx_audit_agreement_id (agreement_id),
    ADD INDEX idx_audit_agreement_version_id (agreement_version_id);

-- ── 8. Recreate foreign keys ────────────────────────────────────────────────
ALTER TABLE agreements
    ADD CONSTRAINT fk_ag_cag FOREIGN KEY (company_agreement_group_id)
        REFERENCES company_agreement_groups (id);

ALTER TABLE agreement_versions
    ADD CONSTRAINT fk_av_agreement FOREIGN KEY (agreement_id)
        REFERENCES agreements (id);

ALTER TABLE agreement_purchase_slabs
    ADD CONSTRAINT fk_aps_agreement_version FOREIGN KEY (agreement_version_id)
        REFERENCES agreement_versions (id);

ALTER TABLE agreement_sale_targets
    ADD CONSTRAINT fk_ast_agreement_version FOREIGN KEY (agreement_version_id)
        REFERENCES agreement_versions (id);

ALTER TABLE agreement_manufacturers
    ADD CONSTRAINT fk_am_agreement_version FOREIGN KEY (agreement_version_id)
        REFERENCES agreement_versions (id);

ALTER TABLE agreement_vendors
    ADD CONSTRAINT fk_av_agreement_version FOREIGN KEY (agreement_version_id)
        REFERENCES agreement_versions (id);

ALTER TABLE agreement_approvals
    ADD CONSTRAINT fk_aa_agreement_version FOREIGN KEY (agreement_version_id)
        REFERENCES agreement_versions (id);

ALTER TABLE agreement_reminders
    ADD CONSTRAINT fk_ar_agreement_version FOREIGN KEY (agreement_version_id)
        REFERENCES agreement_versions (id);

ALTER TABLE agreement_action_requests
    ADD CONSTRAINT fk_aar_agreement_version FOREIGN KEY (agreement_version_id)
        REFERENCES agreement_versions (id);

ALTER TABLE agreement_product_rules
    ADD CONSTRAINT fk_apr_agreement_version FOREIGN KEY (agreement_version_id)
        REFERENCES agreement_versions (id);

ALTER TABLE agreement_division_rules
    ADD CONSTRAINT fk_adr_agreement_version FOREIGN KEY (agreement_version_id)
        REFERENCES agreement_versions (id);

ALTER TABLE agreement_computed_products
    ADD CONSTRAINT fk_acp_agreement_version FOREIGN KEY (agreement_version_id)
        REFERENCES agreement_versions (id);

ALTER TABLE agreement_documents
    ADD CONSTRAINT fk_adoc_agreement_version FOREIGN KEY (agreement_version_id)
        REFERENCES agreement_versions (id);

SET FOREIGN_KEY_CHECKS = 1;
