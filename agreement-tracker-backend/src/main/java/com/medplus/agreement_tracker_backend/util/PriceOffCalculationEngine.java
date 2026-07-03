package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.enums.PriceOffDiscountType;
import com.medplus.agreement_tracker_backend.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceOffCalculationEngine {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private PriceOffCalculationEngine() {}

    public record CalculatedFields(
            BigDecimal marginPercent,
            BigDecimal finalOffer,
            BigDecimal percentOff,
            BigDecimal finalMarginPercent
    ) {}

    public static CalculatedFields calculate(
            PriceOffDiscountType discountType,
            BigDecimal cp,
            BigDecimal mrp,
            BigDecimal baseOffer,
            BigDecimal medplusContribution) {
        validatePositive(mrp, "MRP");
        validatePositive(cp, "CP");
        if (baseOffer == null) {
            throw new BusinessException("Base Offer is required");
        }
        if (medplusContribution == null) {
            throw new BusinessException("Medplus Contribution is required");
        }

        BigDecimal normalizedBaseOffer = normalizeContribution(baseOffer, discountType);
        BigDecimal normalizedMedplusContribution = normalizeContribution(medplusContribution, discountType);

        BigDecimal margin = mrp.subtract(cp).divide(mrp, SCALE, ROUNDING);
        BigDecimal finalOffer = normalizedBaseOffer.add(normalizedMedplusContribution);

        BigDecimal percentOff;
        BigDecimal finalMargin;
        if (discountType == PriceOffDiscountType.DISC_PERCENT) {
            percentOff = finalOffer;
            finalMargin = margin.subtract(normalizedMedplusContribution);
        } else {
            percentOff = finalOffer.divide(mrp, SCALE, ROUNDING);
            finalMargin = margin.subtract(normalizedMedplusContribution.divide(mrp, SCALE, ROUNDING));
        }

        return new CalculatedFields(margin, finalOffer, percentOff, finalMargin);
    }

    private static BigDecimal normalizeContribution(BigDecimal value, PriceOffDiscountType discountType) {
        if (discountType != PriceOffDiscountType.DISC_PERCENT) {
            return value;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return value.divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
        }
        return value;
    }

    private static void validatePositive(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessException(label + " must be greater than zero");
        }
    }
}
