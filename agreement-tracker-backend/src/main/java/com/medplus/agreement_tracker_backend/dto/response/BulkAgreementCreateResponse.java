package com.medplus.agreement_tracker_backend.dto.response;

import java.util.List;

public record BulkAgreementCreateResponse(
        List<AgreementResponse> agreements,
        Long primaryAgreementGroupId
) {}
