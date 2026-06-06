package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateAgreementRequest(
        @NotNull(message = "Company is required")
        Long companyId,

        @NotEmpty(message = "At least one vendor is required")
        List<Long> vendorIds,

        @NotNull @Valid
        ProductRulesPayload productRules,

        @NotEmpty(message = "At least one agreement is required")
        List<@Valid BulkAgreementItemRequest> agreements
) {}
