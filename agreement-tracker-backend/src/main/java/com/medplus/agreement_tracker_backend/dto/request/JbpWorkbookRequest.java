package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record JbpWorkbookRequest(
        @NotEmpty List<@NotBlank String> selectedFrequencies,
        @NotEmpty List<@NotNull JbpConfigurationBlockDto> configurations,
        @NotNull @Min(1) @Max(12) Integer financialYearStartMonth
) {
}
