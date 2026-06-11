package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkTransferRequest(
        @NotNull(message = "Source user is required") Long fromUserId,
        @NotNull(message = "Target user is required") Long toUserId,
        List<Long> specificAgreementIds
) {}
