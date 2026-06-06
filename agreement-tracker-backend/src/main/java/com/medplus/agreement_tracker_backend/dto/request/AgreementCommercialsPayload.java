package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.enums.CommercialStructure;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AgreementCommercialsPayload(
        @NotNull(message = "Commercial structure is required")
        CommercialStructure commercialStructure,

        BigDecimal commercialValue,

        String calculationFormula
) {}
