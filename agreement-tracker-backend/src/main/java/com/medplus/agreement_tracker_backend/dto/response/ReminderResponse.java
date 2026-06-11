package com.medplus.agreement_tracker_backend.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReminderResponse(
        Long id,
        String reminderType,
        Long agreementId,
        String agreementName,
        Long agreementVersionId,
        LocalDate expiryDate,
        LocalDateTime sentAt
) {}
