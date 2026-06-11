-- Support group-level DELETE_GROUP action requests

ALTER TABLE agreement_action_requests
    MODIFY COLUMN agreement_version_id BIGINT NULL;

ALTER TABLE agreement_action_requests
    ADD COLUMN company_agreement_group_id BIGINT NULL,
    ADD CONSTRAINT fk_aar_company_agreement_group
        FOREIGN KEY (company_agreement_group_id) REFERENCES company_agreement_groups (id);

CREATE INDEX idx_aar_company_agreement_group_id ON agreement_action_requests (company_agreement_group_id);
