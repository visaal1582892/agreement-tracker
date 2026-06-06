package com.medplus.agreement_tracker_backend.dto.request;

import java.time.LocalDate;

public record DraftDetailsPayload(
        Long incomeTypeId,
        Long agreementTypeId,
        LocalDate startDate,
        LocalDate expiryDate,
        String notes
) {}
