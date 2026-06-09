package com.medplus.agreement_tracker_backend.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record TimePeriodTargetsPreviewResponse(
        Long timePeriodId,
        String name,
        Map<Long, BigDecimal> targets
) {}
