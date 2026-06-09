package com.medplus.agreement_tracker_backend.dto.response;

import com.medplus.agreement_tracker_backend.enums.SlabValueType;

import java.math.BigDecimal;

public record PurchaseSlabResponse(
        Long id,
        BigDecimal fromValue,
        BigDecimal toValue,
        SlabValueType valueType,
        BigDecimal commercialValue
) {}
