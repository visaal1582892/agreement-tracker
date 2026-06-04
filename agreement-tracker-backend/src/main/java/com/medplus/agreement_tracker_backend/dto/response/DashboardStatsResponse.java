package com.medplus.agreement_tracker_backend.dto.response;

public record DashboardStatsResponse(
        long totalActive,
        long expiringIn30Days,
        long expiringIn60Days,
        long expiringIn90Days,
        long expired,
        long pendingMyApproval,
        long inProgress,
        long totalTerminated
) {}
