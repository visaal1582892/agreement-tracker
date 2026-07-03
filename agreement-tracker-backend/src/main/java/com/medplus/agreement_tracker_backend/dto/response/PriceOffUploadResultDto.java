package com.medplus.agreement_tracker_backend.dto.response;

import java.util.List;

public record PriceOffUploadResultDto(
        int createdCount,
        int skippedCount,
        List<PriceOffUploadError> errors
) {
    public record PriceOffUploadError(int rowNumber, String message) {}
}
