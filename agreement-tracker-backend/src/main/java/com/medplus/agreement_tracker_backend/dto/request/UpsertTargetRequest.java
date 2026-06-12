package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpsertTargetRequest(
        @NotNull Long timePeriodId,
        @NotNull Long slabId,
        BigDecimal targetValue,
        CommercialSlabType targetType
) {}
