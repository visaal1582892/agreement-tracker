package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.validation.Step1Validation;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateAgreementRequest(
        @NotNull(message = "Company is required", groups = Step1Validation.class)
        Long companyId,

        Long companyAgreementGroupId,

        String newCompanyAgreementGroupName,

        List<Long> vendorIds,

        ProductRulesPayload productRules,

        List<DraftAgreementItemRequest> agreements
) {}
