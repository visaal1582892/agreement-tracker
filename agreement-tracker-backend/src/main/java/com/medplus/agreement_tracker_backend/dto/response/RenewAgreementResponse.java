package com.medplus.agreement_tracker_backend.dto.response;

public record RenewAgreementResponse(
        Long versionId,
        Long agreementId,
        Long groupId
) {}
