package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkPriceOffRejectRequest(
        @NotEmpty List<Long> ids,
        @NotBlank @Size(max = 1000) String remarks
) {}
