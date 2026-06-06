package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EditAgreementRequest(
        @NotEmpty(message = "At least one vendor is required")
        List<Long> vendorIds,

        @NotNull @Valid
        ProductRulesPayload productRules,

        @NotNull @Valid
        AgreementDetailsPayload details,

        @NotNull @Valid
        AgreementCommercialsPayload commercials
) {}
