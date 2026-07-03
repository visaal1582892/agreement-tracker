package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.dto.response.StagedMatrixRowDto;
import com.medplus.agreement_tracker_backend.dto.response.StagedSlabHeaderDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CommitCutoffsRequest(
        @NotNull @Valid List<StagedMatrixRowDto> matrixRows,
        List<StagedSlabHeaderDto> slabHeaders
) {}
