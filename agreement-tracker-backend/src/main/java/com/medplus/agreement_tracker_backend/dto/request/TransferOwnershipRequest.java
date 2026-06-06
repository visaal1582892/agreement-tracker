package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record TransferOwnershipRequest(
        @NotNull(message = "New owner user id is required") Long newOwnerUserId
) {}
