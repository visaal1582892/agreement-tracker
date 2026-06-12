-- Generalize purchase/sale-specific slab & target tables with type columns.

RENAME TABLE agreement_purchase_slabs TO agreement_slabs;

ALTER TABLE agreement_slabs
    ADD COLUMN slab_type ENUM('SALE', 'PURCHASE') NOT NULL DEFAULT 'PURCHASE' AFTER commercial_value;

RENAME TABLE agreement_sale_targets TO agreement_targets;

ALTER TABLE agreement_targets
    ADD COLUMN target_type ENUM('SALE', 'PURCHASE') NOT NULL DEFAULT 'SALE' AFTER target_value;
