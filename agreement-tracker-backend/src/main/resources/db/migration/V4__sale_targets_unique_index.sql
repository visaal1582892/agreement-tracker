ALTER TABLE agreement_sale_targets
    ADD CONSTRAINT uk_target_coordinate UNIQUE (agreement_id, time_period_id, slab_id);
