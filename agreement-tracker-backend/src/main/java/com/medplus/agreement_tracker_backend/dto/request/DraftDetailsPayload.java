package com.medplus.agreement_tracker_backend.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DraftDetailsPayload(
        Long incomeTypeId,
        Long agreementTypeId,
        LocalDate startDate,
        LocalDate expiryDate,
        String notes,
        List<Long> stateIds,
        String adhocSubType,
        BigDecimal quantityCap,
        Long invoiceVendorId,
        Integer payoutBufferDays,
        String calculationBasis,
        String paymentRealizationType
) {}
