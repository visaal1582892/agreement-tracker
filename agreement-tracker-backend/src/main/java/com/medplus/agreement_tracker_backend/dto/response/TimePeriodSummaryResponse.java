package com.medplus.agreement_tracker_backend.dto.response;

public record TimePeriodSummaryResponse(
        Long id,
        String name,
        String periodFrequency,
        Integer periodYear,
        Integer monthNumber
) {
}
