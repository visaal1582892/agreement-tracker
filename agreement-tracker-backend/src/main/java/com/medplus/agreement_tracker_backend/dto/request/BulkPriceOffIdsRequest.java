package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkPriceOffIdsRequest(
        @NotEmpty List<Long> ids
) {}
