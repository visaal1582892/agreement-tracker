package com.medplus.agreement_tracker_backend.enums;

/**
 * Application permission codes. Stored in {@code rights.code} and issued to users
 * via role assignments. Designed to map cleanly to CAS attributes during SSO integration.
 */
public enum RightCode {
    DASHBOARD_VIEW,
    AGREEMENT_VIEW,
    AGREEMENT_VIEW_ALL,
    AGREEMENT_CREATE,
    AGREEMENT_EDIT,
    AGREEMENT_APPROVE,
    MASTER_VIEW,
    MASTER_MANAGE,
    ADMIN_USERS
}
