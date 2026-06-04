package com.medplus.agreement_tracker_backend.dto.response;

import com.medplus.agreement_tracker_backend.enums.AgreementStatus;

import java.time.LocalDateTime;

public record AgreementGroupResponse(
        Long id,
        String agreementNumber,
        Long companyId,
        String companyName,
        Long currentVersionId,
        Integer currentVersionNumber,
        AgreementStatus currentStatus,
        boolean isActive,
        LocalDateTime createdAt
) {}
