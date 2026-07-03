package com.medplus.agreement_tracker_backend.dto.response;

import java.util.Map;

public record StagedMatrixRowDto(
        String timePeriodName,
        Long timePeriodId,
        Map<String, StagedTierCutoffDto> tierCutoffs
) {}
