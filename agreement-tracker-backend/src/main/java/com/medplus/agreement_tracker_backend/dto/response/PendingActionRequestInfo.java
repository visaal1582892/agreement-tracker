package com.medplus.agreement_tracker_backend.dto.response;

import com.medplus.agreement_tracker_backend.enums.ActionRequestType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PendingActionRequestInfo(
        Long requestId,
        ActionRequestType actionType,
        String reasonComments,
        LocalDate requestedTerminationDate,
        Long targetUserId,
        String targetUserName,
        String requestedByName,
        LocalDateTime createdAt
) {}
