package com.medplus.agreement_tracker_backend.dto.response;

public record CommercialUploadResponse(
        int savedCount,
        int skippedRows,
        int skippedCells
) {}
