package com.medplus.agreement_tracker_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreUploadResultDto {

    private List<AgreementStoreMappingResponse> successfullyMapped = new ArrayList<>();
    private List<StoreUploadError> skippedStores = new ArrayList<>();
    private int totalAttempted;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreUploadError {
        private String storeCode;
        private String reason;
    }
}
