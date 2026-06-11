package com.medplus.agreement_tracker_backend.dto.response;

import java.time.LocalDate;

public record ExpiringAgreementResponse(
        Long agreementVersionId,
        Long agreementId,
        String agreementName,
        String companyName,
        String ownerName,
        LocalDate expiryDate,
        long daysUntilExpiry,
        String urgency
) {}
