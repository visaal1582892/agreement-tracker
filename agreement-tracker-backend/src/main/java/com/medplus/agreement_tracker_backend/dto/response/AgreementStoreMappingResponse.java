package com.medplus.agreement_tracker_backend.dto.response;

public record AgreementStoreMappingResponse(
        Long mappingId,
        Long storeId,
        String storeCode,
        String storeName,
        Long stateId,
        String stateName
) {}
