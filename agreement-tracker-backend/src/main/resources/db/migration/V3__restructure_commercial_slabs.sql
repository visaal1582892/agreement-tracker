-- Phase 1: Commercial slabs restructure — lean global time periods, agreement-scoped slabs & sale targets.
-- Fresh start approved: old commercial slab data is discarded.

DROP TABLE IF EXISTS slab_period_values;
DROP TABLE IF EXISTS agreement_sale_targets;
DROP TABLE IF EXISTS agreement_purchase_slabs;
DROP TABLE IF EXISTS agreement_slabs;
DROP TABLE IF EXISTS agreement_time_periods;

CREATE TABLE agreement_time_periods (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    name                VARCHAR(100)    NOT NULL,
    created_by_user_id  BIGINT          NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by_user_id  BIGINT          NULL,
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_atp_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agreement_purchase_slabs (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    agreement_id        BIGINT          NOT NULL,
    from_value          DECIMAL(15, 2)  NOT NULL,
    to_value            DECIMAL(15, 2)  NOT NULL,
    value_type          ENUM('PERCENTAGE', 'FIXED') NOT NULL,
    commercial_value    DECIMAL(15, 2)  NOT NULL,
    created_by_user_id  BIGINT          NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by_user_id  BIGINT          NULL,
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_aps_agreement FOREIGN KEY (agreement_id) REFERENCES agreements (id),
    INDEX idx_aps_agreement_id (agreement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agreement_sale_targets (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    agreement_id        BIGINT          NOT NULL,
    time_period_id      BIGINT          NOT NULL,
    slab_id             BIGINT          NOT NULL,
    target_value        DECIMAL(15, 2)  NOT NULL,
    created_by_user_id  BIGINT          NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by_user_id  BIGINT          NULL,
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_ast_agreement FOREIGN KEY (agreement_id) REFERENCES agreements (id),
    CONSTRAINT fk_ast_time_period FOREIGN KEY (time_period_id) REFERENCES agreement_time_periods (id),
    CONSTRAINT fk_ast_slab FOREIGN KEY (slab_id) REFERENCES agreement_purchase_slabs (id),
    INDEX idx_ast_agreement_id (agreement_id),
    INDEX idx_ast_time_period_id (time_period_id),
    INDEX idx_ast_slab_id (slab_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
