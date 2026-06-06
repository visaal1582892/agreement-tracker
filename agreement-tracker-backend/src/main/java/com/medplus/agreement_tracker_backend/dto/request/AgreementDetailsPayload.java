package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AgreementDetailsPayload(
        @NotNull(message = "Income type is required")
        Long incomeTypeId,

        @NotNull(message = "Agreement type is required")
        Long agreementTypeId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "Expiry date is required")
        LocalDate expiryDate,

        String notes
) {}
