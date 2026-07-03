package com.medplus.agreement_tracker_backend.dto.response;

import java.math.BigDecimal;

public record SlabPeriodCutoffRowResponse(
        Long timePeriodId,
        String timePeriodName,
        Integer periodYear,
        Integer monthNumber,
        Long slabId,
        String slabTierLabel,
        BigDecimal minCap,
        BigDecimal maxCap,
        BigDecimal lowerCutoff,
        BigDecimal upperCutoff,
        BigDecimal payoutValue,
        String payoutValueType
) {}
