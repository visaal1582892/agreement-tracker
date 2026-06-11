package com.medplus.agreement_tracker_backend.dto.response;

import com.medplus.agreement_tracker_backend.enums.AgreementStatus;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AgreementResponse(
        Long id,
        String agreementName,
        Long companyId,
        String companyName,
        Long companyAgreementGroupId,
        String companyAgreementGroupName,
        Long agreementTypeId,
        String agreementTypeName,
        Long currentVersionId,
        Long latestVersionId,
        Integer currentVersionNumber,
        AgreementStatus computedStatus,
        ApprovalStatus approvalStatus,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String incomeTypeName,
        LocalDate startDate,
        LocalDate expiryDate,
        String ownerName,
        Long ownerUserId,
        List<AgreementVersionResponse.VendorSummary> vendors
) {}
