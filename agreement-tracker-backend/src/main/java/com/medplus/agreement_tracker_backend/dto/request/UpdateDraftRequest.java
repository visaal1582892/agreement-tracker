package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.validation.DraftValidation;
import com.medplus.agreement_tracker_backend.validation.Step1Validation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateDraftRequest(
        @NotBlank(message = "Agreement name is required", groups = { DraftValidation.class, Step1Validation.class })
        String agreementName,

        @NotNull(message = "Company is required", groups = Step1Validation.class)
        Long companyId,
        List<Long> vendorIds,
        ProductRulesPayload productRules,
        DraftDetailsPayload details,
        DraftCommercialsPayload commercials
) {}
