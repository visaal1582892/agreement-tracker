package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.Size;

public record SubmitForApprovalRequest(
        @Size(max = 1000)
        String comments
) {}
