ALTER TABLE agreement_reminders
    ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE agreement_versions
    ADD COLUMN in_progress_since DATETIME(6) NULL;

ALTER TABLE agreement_reminders
    MODIFY COLUMN reminder_type VARCHAR(50) NOT NULL;
