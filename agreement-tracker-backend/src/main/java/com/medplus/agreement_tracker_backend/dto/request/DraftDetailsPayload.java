package com.medplus.agreement_tracker_backend.dto.request;

import java.time.LocalDate;
import java.util.List;

public record DraftDetailsPayload(
        Long incomeTypeId,
        Long agreementTypeId,
        LocalDate startDate,
        LocalDate expiryDate,
        String notes,
        List<Long> stateIds
) {}
