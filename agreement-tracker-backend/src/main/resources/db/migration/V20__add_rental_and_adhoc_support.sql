-- Asset rental, ad-hoc (QPS / price off), and store-outlet document support.

ALTER TABLE agreement_documents
    MODIFY COLUMN document_type
        ENUM('AGREEMENT', 'EMAIL', 'OTHER', 'SUPPORTING_DOC', 'TERMINATION', 'STORE_OUTLET_LIST') NOT NULL;

ALTER TABLE agreement_versions
    ADD COLUMN quantity_cap DECIMAL(15, 2) NULL AFTER commercial_value;

CREATE TABLE agreement_assets (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    created_by_user_id      BIGINT          NULL,
    created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by_user_id      BIGINT          NULL,
    updated_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    agreement_version_id    BIGINT          NOT NULL,
    asset_category          ENUM('PHYSICAL_ASSET', 'ACTIVITY') NOT NULL,
    asset_type              VARCHAR(100)    NOT NULL,
    store_count             INT             NULL,
    payout_per_store        DECIMAL(15, 2)  NULL,
    flat_payout             DECIMAL(15, 2)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_agreement_assets_version UNIQUE (agreement_version_id),
    CONSTRAINT fk_agreement_assets_version FOREIGN KEY (agreement_version_id) REFERENCES agreement_versions (id),
    INDEX idx_aa_agreement_version_id (agreement_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO income_types (name, is_active, created_at, updated_at)
SELECT seed.name, 1, NOW(6), NOW(6)
FROM (
    SELECT 'Data Fee' AS name
    UNION ALL SELECT 'Commercial Contracts'
    UNION ALL SELECT 'Asset Rentals'
    UNION ALL SELECT 'Ad-Hoc Activities'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM income_types existing WHERE LOWER(existing.name) = LOWER(seed.name)
);
