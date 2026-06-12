package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import com.medplus.agreement_tracker_backend.enums.SlabValueType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SlabDTO(
        @NotNull BigDecimal fromValue,
        @NotNull BigDecimal toValue,
        @NotNull SlabValueType valueType,
        @NotNull BigDecimal commercialValue,
        CommercialSlabType slabType
) {}
