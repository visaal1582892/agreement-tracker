package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.enums.CommercialStructure;

import java.math.BigDecimal;

public record DraftCommercialsPayload(
        CommercialStructure commercialStructure,
        BigDecimal commercialValue,
        String calculationFormula
) {}
