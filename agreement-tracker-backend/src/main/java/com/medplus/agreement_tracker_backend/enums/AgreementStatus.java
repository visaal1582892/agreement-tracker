package com.medplus.agreement_tracker_backend.enums;

/**
 * Derived status — not stored in DB. Computed dynamically from Agreement fields.
 * Termination date not null -> TERMINATED
 * in_progress_flag true -> IN_PROGRESS
 * expiry_date < today -> EXPIRED
 * otherwise -> ACTIVE (for APPROVED versions)
 */
public enum AgreementStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    ACTIVE,
    EXPIRED,
    TERMINATED,
    IN_PROGRESS
}
