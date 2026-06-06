package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateAgreementRequest(
        @NotNull(message = "Company is required")
        Long companyId,

        List<Long> vendorIds,

        ProductRulesPayload productRules,

        List<DraftAgreementItemRequest> agreements
) {}
