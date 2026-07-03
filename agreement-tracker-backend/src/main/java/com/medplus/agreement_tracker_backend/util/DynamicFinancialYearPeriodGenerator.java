package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DynamicFinancialYearPeriodGenerator {

    private DynamicFinancialYearPeriodGenerator() {
    }

    public static int resolveStartMonth(Integer configuredStartMonth) {
        if (configuredStartMonth == null || configuredStartMonth < 1 || configuredStartMonth > 12) {
            return 4;
        }
        return configuredStartMonth;
    }

    public static List<String> generatePeriodNames(
            PayoutFrequency frequency,
            LocalDate contractStart,
            LocalDate contractEnd,
            int financialYearStartMonth) {
        if (contractStart == null || contractEnd == null || frequency == null) {
            return List.of();
        }
        return switch (frequency) {
            case ONE_TIME -> List.of("ONE_TIME");
            case MONTHLY -> generateMonthlyPeriods(contractStart, contractEnd);
            case QUARTERLY -> generateQuarterlyPeriods(contractStart, contractEnd, financialYearStartMonth);
            case HALF_YEARLY -> generateHalfYearlyPeriods(contractStart, contractEnd, financialYearStartMonth);
            case YEARLY -> generateYearlyPeriods(contractStart, contractEnd, financialYearStartMonth);
            default -> List.of();
        };
    }

    public static List<String> generateMonthlyPeriods(LocalDate contractStart, LocalDate contractEnd) {
        List<String> periods = new ArrayList<>();
        YearMonth current = YearMonth.from(contractStart);
        YearMonth last = YearMonth.from(contractEnd);
        while (!current.isAfter(last)) {
            periods.add(formatMonthlyName(current));
            current = current.plusMonths(1);
        }
        return periods;
    }

    public static List<String> generateQuarterlyPeriods(
            LocalDate contractStart,
            LocalDate contractEnd,
            int financialYearStartMonth) {
        return generateFyGroupedPeriods(contractStart, contractEnd, financialYearStartMonth, 3, 4);
    }

    public static List<String> generateHalfYearlyPeriods(
            LocalDate contractStart,
            LocalDate contractEnd,
            int financialYearStartMonth) {
        return generateFyGroupedPeriods(contractStart, contractEnd, financialYearStartMonth, 6, 2);
    }

    public static List<String> generateYearlyPeriods(
            LocalDate contractStart,
            LocalDate contractEnd,
            int financialYearStartMonth) {
        List<String> periods = new ArrayList<>();
        YearMonth contractStartYm = YearMonth.from(contractStart);
        YearMonth contractEndYm = YearMonth.from(contractEnd);
        YearMonth fyStart = alignToFinancialYearStart(contractStartYm, financialYearStartMonth);

        while (!fyStart.isAfter(contractEndYm)) {
            YearMonth fyEnd = fyStart.plusMonths(11);
            if (periodOverlapsContract(fyStart, fyEnd, contractStartYm, contractEndYm)) {
                periods.add(formatFinancialYearName(fyStart, financialYearStartMonth));
            }
            fyStart = fyStart.plusMonths(12);
        }
        return periods;
    }

    private static List<String> generateFyGroupedPeriods(
            LocalDate contractStart,
            LocalDate contractEnd,
            int financialYearStartMonth,
            int monthsPerPeriod,
            int periodsPerFy) {
        List<String> periods = new ArrayList<>();
        YearMonth contractStartYm = YearMonth.from(contractStart);
        YearMonth contractEndYm = YearMonth.from(contractEnd);
        YearMonth fyAnchor = alignToFinancialYearStart(contractStartYm, financialYearStartMonth);

        while (!fyAnchor.isAfter(contractEndYm)) {
            int fyAnchorYear = fyAnchor.getYear();
            for (int index = 0; index < periodsPerFy; index++) {
                YearMonth periodStart = fyAnchor.plusMonths((long) index * monthsPerPeriod);
                YearMonth periodEnd = periodStart.plusMonths(monthsPerPeriod - 1L);
                if (periodOverlapsContract(periodStart, periodEnd, contractStartYm, contractEndYm)) {
                    periods.add(formatGroupedMonthsName(
                            periodStart, monthsPerPeriod, fyAnchorYear, financialYearStartMonth));
                }
            }
            fyAnchor = fyAnchor.plusMonths(12);
        }
        return periods;
    }

    public static String formatMonthlyName(YearMonth yearMonth) {
        return String.format("%02d-%04d", yearMonth.getMonthValue(), yearMonth.getYear());
    }

    public static String formatGroupedMonthsName(YearMonth start, int monthCount) {
        return formatGroupedMonthsName(start, monthCount, start.getYear(), start.getMonthValue());
    }

    public static String formatGroupedMonthsName(
            YearMonth start,
            int monthCount,
            int fyAnchorYear,
            int financialYearStartMonth) {
        int displayYear = resolveDisplayYear(start.getMonthValue(), fyAnchorYear, financialYearStartMonth);

        if (monthCount == 3) {
            YearMonth monthTwo = start.plusMonths(1);
            YearMonth monthThree = start.plusMonths(2);
            return String.format(
                    "%02d-%02d-%02d (%d)",
                    start.getMonthValue(),
                    monthTwo.getMonthValue(),
                    monthThree.getMonthValue(),
                    displayYear);
        }

        StringBuilder builder = new StringBuilder();
        YearMonth cursor = start;
        for (int index = 0; index < monthCount; index++) {
            if (index > 0) {
                builder.append('-');
            }
            builder.append(String.format("%02d", cursor.getMonthValue()));
            cursor = cursor.plusMonths(1);
        }
        builder.append(" (").append(displayYear).append(')');
        return builder.toString();
    }

    public static String formatFinancialYearName(YearMonth fyStart, int financialYearStartMonth) {
        int startYear = fyStart.getYear();
        String monthAbbr = formatStartMonthAbbreviation(financialYearStartMonth);
        return "FY " + startYear + "-" + (startYear + 1) + " (Starts " + monthAbbr + ")";
    }

    public static String formatStartMonthAbbreviation(int financialYearStartMonth) {
        return Month.of(financialYearStartMonth)
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    static int resolveDisplayYear(int blockStartMonth, int fyAnchorYear, int financialYearStartMonth) {
        if (blockStartMonth < financialYearStartMonth || (financialYearStartMonth != 1 && blockStartMonth == 1)) {
            return fyAnchorYear + 1;
        }
        return fyAnchorYear;
    }

    public static YearMonth alignToQuarterStart(YearMonth anchor, int financialYearStartMonth) {
        YearMonth fyStart = alignToFinancialYearStart(anchor, financialYearStartMonth);
        YearMonth periodStart = fyStart;
        for (int index = 0; index < 4; index++) {
            YearMonth periodEnd = periodStart.plusMonths(2);
            if (!anchor.isBefore(periodStart) && !anchor.isAfter(periodEnd)) {
                return periodStart;
            }
            periodStart = periodStart.plusMonths(3);
        }
        return fyStart;
    }

    public static YearMonth alignToHalfYearStart(YearMonth anchor, int financialYearStartMonth) {
        YearMonth fyStart = alignToFinancialYearStart(anchor, financialYearStartMonth);
        YearMonth secondHalfStart = fyStart.plusMonths(6);
        if (!anchor.isBefore(secondHalfStart)) {
            return secondHalfStart;
        }
        return fyStart;
    }

    public static YearMonth alignToFinancialYearStart(YearMonth anchor, int financialYearStartMonth) {
        YearMonth candidate = YearMonth.of(anchor.getYear(), financialYearStartMonth);
        if (anchor.isBefore(candidate)) {
            return candidate.minusYears(1);
        }
        return candidate;
    }

    private static boolean periodOverlapsContract(
            YearMonth periodStart,
            YearMonth periodEnd,
            YearMonth contractStart,
            YearMonth contractEnd) {
        return !periodEnd.isBefore(contractStart) && !periodStart.isAfter(contractEnd);
    }
}
