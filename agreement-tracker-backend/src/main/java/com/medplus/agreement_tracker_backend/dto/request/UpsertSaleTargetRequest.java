package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpsertSaleTargetRequest(
        @NotNull Long timePeriodId,
        @NotNull Long slabId,
        BigDecimal targetValue
) {}
