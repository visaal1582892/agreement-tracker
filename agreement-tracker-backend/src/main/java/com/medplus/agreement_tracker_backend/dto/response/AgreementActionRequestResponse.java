package com.medplus.agreement_tracker_backend.dto.response;

import com.medplus.agreement_tracker_backend.enums.ActionRequestStatus;
import com.medplus.agreement_tracker_backend.enums.ActionRequestType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AgreementActionRequestResponse(
        Long id,
        Long agreementId,
        Long agreementGroupId,
        String agreementNumber,
        String agreementName,
        String companyName,
        ActionRequestType actionType,
        ActionRequestStatus status,
        Long requestedByUserId,
        String requestedByName,
        Long targetUserId,
        String targetUserName,
        String reasonComments,
        LocalDate requestedTerminationDate,
        String approverComments,
        Long resolvedByUserId,
        String resolvedByName,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {}
