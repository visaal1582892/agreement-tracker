package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SlabDefinitionRequest(
        @NotBlank String slabName,
        @NotNull BigDecimal fromValue,
        @NotNull BigDecimal toValue,
        Integer displayOrder
) {}
