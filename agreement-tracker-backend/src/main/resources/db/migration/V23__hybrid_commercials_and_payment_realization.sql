-- Hybrid commercial structure, per-slab payout frequency, flat baseline fields, payment realization.

ALTER TABLE agreement_versions
    MODIFY COLUMN commercial_structure ENUM('FLAT', 'SLAB', 'HYBRID') NULL,
    ADD COLUMN flat_value_type ENUM('PERCENTAGE', 'FIXED') NULL AFTER commercial_value,
    ADD COLUMN flat_baseline_frequency ENUM('ONE_TIME', 'MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY') NULL AFTER flat_value_type,
    ADD COLUMN payment_realization_type ENUM('DIRECT_PAYMENT_INVOICE', 'CREDIT_NOTE', 'INVOICE_DISCOUNT') NOT NULL DEFAULT 'DIRECT_PAYMENT_INVOICE'
        AFTER calculation_basis;

ALTER TABLE agreement_slabs
    ADD COLUMN payout_frequency ENUM('ONE_TIME', 'MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY') NULL AFTER commercial_value;
