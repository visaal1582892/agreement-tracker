package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.entity.AgreementPurchaseSlab;
import com.medplus.agreement_tracker_backend.enums.SlabValueType;

import java.math.BigDecimal;

public final class SlabHeaderFormatter {

    private SlabHeaderFormatter() {}

    public static String format(SlabDTO slab) {
        return format(slab.fromValue(), slab.toValue(), slab.valueType(), slab.commercialValue());
    }

    public static String format(AgreementPurchaseSlab slab) {
        return format(slab.getFromValue(), slab.getToValue(), slab.getValueType(), slab.getCommercialValue());
    }

    public static String format(BigDecimal fromValue, BigDecimal toValue, SlabValueType valueType, BigDecimal commercialValue) {
        String range = formatNumber(fromValue) + " - " + formatNumber(toValue);
        if (valueType == SlabValueType.PERCENTAGE) {
            return range + " (" + formatNumber(commercialValue) + "%)";
        }
        return range + " (Fixed: " + formatNumber(commercialValue) + ")";
    }

    private static String formatNumber(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
