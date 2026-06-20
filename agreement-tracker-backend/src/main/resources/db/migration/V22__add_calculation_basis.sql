-- Calculation basis for settlement math (invoice vs inward/GRN).

ALTER TABLE agreement_versions
    ADD COLUMN calculation_basis ENUM('VENDOR_INVOICE', 'VENDOR_INWARD') NOT NULL DEFAULT 'VENDOR_INVOICE'
        AFTER payout_buffer_days;
