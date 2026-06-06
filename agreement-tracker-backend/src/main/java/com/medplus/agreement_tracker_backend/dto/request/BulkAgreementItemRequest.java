package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record BulkAgreementItemRequest(
        @NotNull @Valid AgreementDetailsPayload details,
        @NotNull @Valid AgreementCommercialsPayload commercials
) {}
