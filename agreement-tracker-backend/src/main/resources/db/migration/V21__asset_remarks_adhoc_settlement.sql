-- Asset rental remarks, ad-hoc sub-types, and settlement routing fields.

ALTER TABLE agreement_assets
    ADD COLUMN remarks VARCHAR(1000) NULL AFTER flat_payout;

ALTER TABLE agreement_versions
    ADD COLUMN adhoc_sub_type ENUM('QPS', 'CONSUMER_PRICE_OFF') NULL AFTER quantity_cap,
    ADD COLUMN invoice_vendor_id BIGINT NULL AFTER adhoc_sub_type,
    ADD COLUMN payout_buffer_days INT NULL AFTER invoice_vendor_id,
    ADD CONSTRAINT fk_av_invoice_vendor FOREIGN KEY (invoice_vendor_id) REFERENCES vendor_master (id);
