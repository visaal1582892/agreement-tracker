-- JBP threshold/qualifier model + FY start month (prod deployment script)

ALTER TABLE agreement_versions
    ADD COLUMN financial_year_start_month INT NOT NULL DEFAULT 4 AFTER expiry_date;

ALTER TABLE agreement_jbp_configurations
    DROP COLUMN boundary_mode;

DROP TABLE IF EXISTS agreement_jbp_config_sub_modes;

ALTER TABLE agreement_jbp_commercial_periods
    DROP COLUMN lower_limit,
    DROP COLUMN upper_limit,
    DROP COLUMN commercial_value,
    DROP COLUMN boundary_mode,
    DROP COLUMN target_value;

ALTER TABLE agreement_jbp_commercial_periods
    ADD COLUMN target_type ENUM('ABSOLUTE', 'RELATIVE') NOT NULL AFTER jbp_configuration_id,
    ADD COLUMN target DECIMAL(15,2) NOT NULL AFTER target_type,
    ADD COLUMN qualifier_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER target,
    ADD COLUMN payout_type ENUM('ABSOLUTE', 'RELATIVE') NOT NULL AFTER qualifier_percent,
    ADD COLUMN payout DECIMAL(15,2) NOT NULL AFTER payout_type,
    ADD COLUMN max_purchase DECIMAL(15,2) DEFAULT NULL AFTER payout,
    ADD COLUMN max_payout DECIMAL(15,2) DEFAULT NULL AFTER max_purchase;
