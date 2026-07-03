package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.enums.CommercialStructure;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.enums.SlabValueType;

import java.math.BigDecimal;

public record DraftCommercialsPayload(
        CommercialStructure commercialStructure,
        BigDecimal commercialValue,
        SlabValueType flatValueType,
        PayoutFrequency flatBaselineFrequency,
        Boolean enableFlatBaseline,
        Boolean enableSlabIncentives,
        String calculationFormula,
        Integer financialYearStartMonth
) {}
