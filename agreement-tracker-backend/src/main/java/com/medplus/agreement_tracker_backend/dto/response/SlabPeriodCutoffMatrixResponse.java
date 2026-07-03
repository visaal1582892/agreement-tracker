package com.medplus.agreement_tracker_backend.dto.response;

import java.util.List;

public record SlabPeriodCutoffMatrixResponse(
        List<SlabPeriodCutoffRowResponse> rows,
        boolean hasUnmappedCutoffs
) {}
