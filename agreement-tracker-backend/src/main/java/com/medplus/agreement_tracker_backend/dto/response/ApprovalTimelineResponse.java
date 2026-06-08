package com.medplus.agreement_tracker_backend.dto.response;

import com.medplus.agreement_tracker_backend.enums.ApprovalAction;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;

import java.time.LocalDateTime;

public record ApprovalTimelineResponse(
        Long id,
        ApprovalAction action,
        String operationalEvent,
        String remarks,
        ApprovalStatus statusBefore,
        ApprovalStatus statusAfter,
        Long actorUserId,
        String actorName,
        LocalDateTime timestamp
) {}
