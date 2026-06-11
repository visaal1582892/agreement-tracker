package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyAgreementGroupRequest(
        @NotBlank(message = "Group name is required")
        @Size(max = 255, message = "Group name must be at most 255 characters")
        String name
) {}
