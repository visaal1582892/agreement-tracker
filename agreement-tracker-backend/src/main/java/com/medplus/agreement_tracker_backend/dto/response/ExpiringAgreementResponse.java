package com.medplus.agreement_tracker_backend.dto.response;

import java.time.LocalDate;

public record ExpiringAgreementResponse(
        Long agreementId,
        Long agreementGroupId,
        String agreementNumber,
        String companyName,
        String ownerName,
        LocalDate expiryDate,
        long daysUntilExpiry,
        String urgency
) {}
