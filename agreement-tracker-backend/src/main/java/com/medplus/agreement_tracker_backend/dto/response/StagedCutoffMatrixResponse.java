package com.medplus.agreement_tracker_backend.dto.response;

import java.util.List;

public record StagedCutoffMatrixResponse(
        List<StagedMatrixRowDto> matrixRows,
        List<StagedSlabHeaderDto> slabHeaders
) {}
