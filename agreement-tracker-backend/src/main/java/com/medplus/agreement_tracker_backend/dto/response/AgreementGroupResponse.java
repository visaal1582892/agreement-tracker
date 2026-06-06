package com.medplus.agreement_tracker_backend.dto.response;

import com.medplus.agreement_tracker_backend.enums.AgreementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AgreementGroupResponse(
        Long id,
        String agreementNumber,
        Long companyId,
        String companyName,
        Long currentVersionId,
        Long latestVersionId,
        Integer currentVersionNumber,
        AgreementStatus currentStatus,
        boolean isActive,
        LocalDateTime createdAt,
        String incomeTypeName,
        LocalDate startDate,
        LocalDate expiryDate,
        String ownerName,
        Long ownerUserId,
        List<AgreementResponse.VendorSummary> vendors
) {}
