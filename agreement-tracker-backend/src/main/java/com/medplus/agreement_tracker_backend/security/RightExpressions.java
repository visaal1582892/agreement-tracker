package com.medplus.agreement_tracker_backend.security;

/**
 * SpEL-friendly authority expressions for {@code @PreAuthorize}.
 * Mirrors {@link com.medplus.agreement_tracker_backend.enums.RightCode}.
 */
public final class RightExpressions {

    private RightExpressions() {}

    public static final String DASHBOARD_VIEW = "hasAuthority('DASHBOARD_VIEW')";
    public static final String AGREEMENT_VIEW = "hasAuthority('AGREEMENT_VIEW')";
    public static final String AGREEMENT_VIEW_ALL = "hasAuthority('AGREEMENT_VIEW_ALL')";
    public static final String AGREEMENT_CREATE = "hasAuthority('AGREEMENT_CREATE')";
    public static final String AGREEMENT_EDIT = "hasAuthority('AGREEMENT_EDIT')";
    public static final String AGREEMENT_APPROVE = "hasAuthority('AGREEMENT_APPROVE')";
    public static final String MASTER_VIEW = "hasAnyAuthority('MASTER_VIEW', 'MASTER_MANAGE')";
    public static final String MASTER_MANAGE = "hasAuthority('MASTER_MANAGE')";
    public static final String ADMIN_USERS = "hasAuthority('ADMIN_USERS')";
    public static final String MASTER_OR_AGREEMENT_READ =
            "hasAnyAuthority('MASTER_VIEW', 'MASTER_MANAGE', 'AGREEMENT_VIEW', 'AGREEMENT_CREATE')";
}
