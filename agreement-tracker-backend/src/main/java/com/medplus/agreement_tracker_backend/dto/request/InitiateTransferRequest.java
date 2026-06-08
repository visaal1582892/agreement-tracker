package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InitiateTransferRequest(
        @NotNull(message = "New owner is required")
        Long newOwnerId,
        @NotBlank(message = "Reason / comments are required")
        @Size(max = 1000)
        String comments
) {}
