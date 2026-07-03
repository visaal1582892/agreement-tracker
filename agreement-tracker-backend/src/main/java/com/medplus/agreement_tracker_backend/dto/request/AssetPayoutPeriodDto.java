package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AssetPayoutPeriodDto(
        @NotNull @Min(1) Integer periodMonths,
        @NotNull @DecimalMin(value = "0.01") BigDecimal payoutPerStore
) {}
