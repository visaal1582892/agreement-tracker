package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.enums.CapUnit;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.enums.SlabValueType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SlabDTO(
        @NotNull BigDecimal minCap,
        @NotNull BigDecimal maxCap,
        @NotNull CapUnit capUnit,
        @NotNull SlabValueType valueType,
        @NotNull BigDecimal commercialValue,
        @NotNull PayoutFrequency payoutFrequency,
        CommercialSlabType slabType
) {
    @AssertTrue(message = "Max Cap must be strictly greater than Min Cap")
    public boolean isValidCapRange() {
        if (minCap == null || maxCap == null) {
            return true;
        }
        return maxCap.compareTo(minCap) > 0;
    }
}
