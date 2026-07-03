package com.medplus.agreement_tracker_backend.dto.response;

import com.medplus.agreement_tracker_backend.enums.CapUnit;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.enums.SlabValueType;

import java.math.BigDecimal;

public record AgreementSlabResponse(
        Long id,
        CommercialSlabType slabType,
        BigDecimal minCap,
        BigDecimal maxCap,
        CapUnit capUnit,
        SlabValueType valueType,
        BigDecimal commercialValue,
        PayoutFrequency payoutFrequency
) {}
