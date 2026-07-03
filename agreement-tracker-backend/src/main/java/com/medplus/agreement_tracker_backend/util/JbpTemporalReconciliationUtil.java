package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JbpTemporalReconciliationUtil {

    private JbpTemporalReconciliationUtil() {
    }

    public static Optional<AgreementTimePeriod> resolveParentPeriod(
            AgreementTimePeriod subPeriod,
            List<AgreementTimePeriod> candidateParents) {
        if (subPeriod == null || candidateParents == null || candidateParents.isEmpty()) {
            return Optional.empty();
        }
        return candidateParents.stream()
                .filter(parent -> matchesParent(subPeriod, parent))
                .findFirst();
    }

    public static List<AgreementTimePeriod> filterSubPeriodsForParent(
            AgreementTimePeriod parentPeriod,
            List<AgreementTimePeriod> subPeriods) {
        if (parentPeriod == null || subPeriods == null || subPeriods.isEmpty()) {
            return List.of();
        }
        return subPeriods.stream()
                .filter(sub -> matchesParent(sub, parentPeriod))
                .sorted(PERIOD_SORT)
                .toList();
    }

    public static Map<Long, List<AgreementTimePeriod>> groupSubPeriodsByParent(
            List<AgreementTimePeriod> subPeriods,
            List<AgreementTimePeriod> parentPeriods) {
        Map<Long, List<AgreementTimePeriod>> grouped = new LinkedHashMap<>();
        for (AgreementTimePeriod parent : parentPeriods) {
            grouped.put(parent.getId(), filterSubPeriodsForParent(parent, subPeriods));
        }
        return grouped;
    }

    public static int resolveSpreadDivisor(PayoutFrequency masterFrequency, PayoutFrequency subFrequency) {
        if (masterFrequency == null || subFrequency == null) {
            throw new IllegalArgumentException("Master and sub frequencies are required");
        }
        return switch (masterFrequency) {
            case YEARLY -> switch (subFrequency) {
                case HALF_YEARLY -> 2;
                case QUARTERLY -> 4;
                case MONTHLY -> 12;
                default -> throw new IllegalArgumentException(
                        "Unsupported sub-frequency " + subFrequency + " under YEARLY master");
            };
            case HALF_YEARLY -> switch (subFrequency) {
                case QUARTERLY -> 2;
                case MONTHLY -> 6;
                default -> throw new IllegalArgumentException(
                        "Unsupported sub-frequency " + subFrequency + " under HALF_YEARLY master");
            };
            case QUARTERLY -> {
                if (subFrequency != PayoutFrequency.MONTHLY) {
                    throw new IllegalArgumentException(
                            "Unsupported sub-frequency " + subFrequency + " under QUARTERLY master");
                }
                yield 3;
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported master frequency " + masterFrequency + " for spread formulas");
        };
    }

    public static int frequencyRank(PayoutFrequency frequency) {
        return switch (frequency) {
            case YEARLY -> 4;
            case HALF_YEARLY -> 3;
            case QUARTERLY -> 2;
            case MONTHLY -> 1;
            default -> 0;
        };
    }

    public static boolean isSpreadFrequencyOf(PayoutFrequency masterFrequency, PayoutFrequency spreadFrequency) {
        if (masterFrequency == null || spreadFrequency == null) {
            return false;
        }
        return frequencyRank(masterFrequency) > frequencyRank(spreadFrequency);
    }

    private static final Comparator<AgreementTimePeriod> PERIOD_SORT = TimePeriodDimensions.chronologicalComparator();

    private static boolean matchesParent(AgreementTimePeriod subPeriod, AgreementTimePeriod parentPeriod) {
        if (subPeriod.getPeriodFrequency() == null || parentPeriod.getPeriodFrequency() == null) {
            return false;
        }
        if (frequencyRank(parentPeriod.getPeriodFrequency()) <= frequencyRank(subPeriod.getPeriodFrequency())) {
            return false;
        }

        YearMonth subStart = TimePeriodDimensions.periodStart(subPeriod);
        YearMonth subEnd = TimePeriodDimensions.periodEnd(subPeriod);
        YearMonth parentStart = TimePeriodDimensions.periodStart(parentPeriod);
        YearMonth parentEnd = TimePeriodDimensions.periodEnd(parentPeriod);

        return !subStart.isBefore(parentStart) && !subEnd.isAfter(parentEnd);
    }
}
