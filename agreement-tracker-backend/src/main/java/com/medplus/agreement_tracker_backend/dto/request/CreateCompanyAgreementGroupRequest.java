package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCompanyAgreementGroupRequest(
        @NotBlank(message = "Name is required") String name
) {}
