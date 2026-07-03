package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PriceOffRejectRequest(
        @NotBlank @Size(max = 1000) String remarks
) {}
