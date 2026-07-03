package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record JbpConfigurationBlockDto(
        @NotBlank String configId,
        @NotEmpty List<Long> parentPeriodIds,
        @NotNull @Positive Integer slabCount
) {
}
