-- Move operational ownership from company_agreement_groups / version-only to agreements.owner_user_id

ALTER TABLE agreements
    ADD COLUMN owner_user_id BIGINT NULL;

UPDATE agreements a
    INNER JOIN agreement_versions av ON av.id = a.current_version_id
SET a.owner_user_id = av.owner_user_id
WHERE a.current_version_id IS NOT NULL;

UPDATE agreements a
    INNER JOIN (
        SELECT av.agreement_id, av.owner_user_id
        FROM agreement_versions av
        INNER JOIN (
            SELECT agreement_id, MAX(version_number) AS max_ver
            FROM agreement_versions
            GROUP BY agreement_id
        ) latest ON latest.agreement_id = av.agreement_id AND latest.max_ver = av.version_number
    ) src ON src.agreement_id = a.id
SET a.owner_user_id = src.owner_user_id
WHERE a.owner_user_id IS NULL;

UPDATE agreements
SET owner_user_id = created_by_user_id
WHERE owner_user_id IS NULL;

ALTER TABLE agreements
    MODIFY COLUMN owner_user_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_ag_owner_user FOREIGN KEY (owner_user_id) REFERENCES users (id);

CREATE INDEX idx_ag_owner_user_id ON agreements (owner_user_id);

ALTER TABLE company_agreement_groups
    DROP FOREIGN KEY fk_cag_owner_user;

DROP INDEX idx_cag_owner_user_id ON company_agreement_groups;

ALTER TABLE company_agreement_groups
    DROP COLUMN owner_user_id;
