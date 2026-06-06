package com.medplus.agreement_tracker_backend.enums;

/**
 * Derived status — not stored in DB. Computed dynamically from Agreement fields.
 * APPROVED + not current_version_id -> SUPERSEDED
 * APPROVED + current + expired -> EXPIRED
 * APPROVED + current -> ACTIVE
 */
public enum AgreementStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    ACTIVE,
    EXPIRED,
    TERMINATED,
    IN_PROGRESS,
    SUPERSEDED
}
