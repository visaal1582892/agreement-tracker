package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.enums.CommercialStructure;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateAgreementRequest(
        @NotNull(message = "Company is required")
        Long companyId,

        @NotEmpty(message = "At least one vendor is required")
        List<Long> vendorIds,

        @NotEmpty(message = "At least one product is required")
        List<Long> productIds,

        @NotNull(message = "Income type is required")
        Long incomeTypeId,

        @NotNull(message = "Agreement type is required")
        Long agreementTypeId,

        @NotNull(message = "Commercial structure is required")
        CommercialStructure commercialStructure,

        BigDecimal commercialValue,

        String calculationFormula,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "Expiry date is required")
        LocalDate expiryDate,

        String notes
) {}
