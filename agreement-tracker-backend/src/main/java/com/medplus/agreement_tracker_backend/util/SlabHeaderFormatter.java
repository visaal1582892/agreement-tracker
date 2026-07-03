package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.entity.AgreementSlab;
import com.medplus.agreement_tracker_backend.enums.CapUnit;
import com.medplus.agreement_tracker_backend.enums.SlabValueType;

import java.math.BigDecimal;

public final class SlabHeaderFormatter {

    private SlabHeaderFormatter() {}

    public static String format(SlabDTO slab) {
        return format(slab.minCap(), slab.maxCap(), slab.capUnit(), slab.valueType(), slab.commercialValue());
    }

    public static String format(AgreementSlab slab) {
        return format(slab.getMinCap(), slab.getMaxCap(), slab.getCapUnit(), slab.getValueType(), slab.getCommercialValue());
    }

    public static String format(
            BigDecimal minCap,
            BigDecimal maxCap,
            CapUnit capUnit,
            SlabValueType valueType,
            BigDecimal commercialValue) {
        String unitSuffix = capUnit == CapUnit.QUANTITY ? " Qty" : " ₹";
        String range = formatNumber(minCap) + " - " + formatNumber(maxCap) + unitSuffix;
        if (valueType == SlabValueType.PERCENTAGE) {
            return range + " (" + formatNumber(commercialValue) + "%)";
        }
        return range + " (Fixed: " + formatNumber(commercialValue) + ")";
    }

    private static String formatNumber(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
