package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveActionRequest(
        @NotNull(message = "Approved flag is required")
        Boolean approved,
        @Size(max = 1000)
        String approverComments
) {}
