package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriodMonth;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimePeriodDimensions {

    private static final Map<String, Integer> MONTH_ABBREVIATIONS = Map.ofEntries(
            Map.entry("JAN", 1), Map.entry("FEB", 2), Map.entry("MAR", 3),
            Map.entry("APR", 4), Map.entry("MAY", 5), Map.entry("JUN", 6),
            Map.entry("JUL", 7), Map.entry("AUG", 8), Map.entry("SEP", 9),
            Map.entry("OCT", 10), Map.entry("NOV", 11), Map.entry("DEC", 12));

    private static final Pattern MONTHLY_PATTERN = Pattern.compile("^(\\d{2})-(\\d{4})$");
    private static final Pattern GROUPED_MONTHS_PATTERN = Pattern.compile("^(\\d{2}(?:-\\d{2})+)\\s+\\((\\d{4})\\)$");
    private static final Pattern FINANCIAL_YEAR_PATTERN =
            Pattern.compile("^FY\\s+(\\d{4})-(\\d{4})\\s+\\(Starts\\s+([A-Za-z]{3})\\)$");

    private TimePeriodDimensions() {}

    public record Dimensions(PayoutFrequency frequency, Integer monthNumber, Integer periodYear) {}

    public static Dimensions fromGeneratedName(String name, PayoutFrequency frequency, LocalDate anchorDate) {
        List<YearMonth> months = includedMonthsFromName(
                name,
                frequency,
                DynamicFinancialYearPeriodGenerator.resolveStartMonth(null),
                anchorDate);
        if (months.isEmpty()) {
            YearMonth anchor = YearMonth.from(anchorDate);
            return new Dimensions(frequency, anchor.getMonthValue(), anchor.getYear());
        }
        YearMonth earliest = months.stream().min(YearMonth::compareTo).orElse(YearMonth.from(anchorDate));
        return new Dimensions(frequency, earliest.getMonthValue(), earliest.getYear());
    }

    public static List<YearMonth> includedMonthsFromName(
            String name,
            PayoutFrequency frequency,
            int financialYearStartMonth,
            LocalDate anchorDate) {
        if (frequency == PayoutFrequency.ONE_TIME || "ONE_TIME".equals(name)) {
            return List.of(YearMonth.from(anchorDate));
        }

        Matcher monthly = MONTHLY_PATTERN.matcher(name);
        if (monthly.matches()) {
            return List.of(YearMonth.of(
                    Integer.parseInt(monthly.group(2)),
                    Integer.parseInt(monthly.group(1))));
        }

        Matcher grouped = GROUPED_MONTHS_PATTERN.matcher(name);
        if (grouped.matches()) {
            String[] tokens = grouped.group(1).split("-");
            int[] monthTokens = new int[tokens.length];
            for (int index = 0; index < tokens.length; index++) {
                monthTokens[index] = Integer.parseInt(tokens[index]);
            }
            return assignCalendarYears(
                    monthTokens,
                    Integer.parseInt(grouped.group(2)),
                    financialYearStartMonth);
        }

        Matcher financialYear = FINANCIAL_YEAR_PATTERN.matcher(name);
        if (financialYear.matches()) {
            int startYear = Integer.parseInt(financialYear.group(1));
            Integer startMonth = MONTH_ABBREVIATIONS.get(financialYear.group(3).toUpperCase(Locale.ENGLISH));
            if (startMonth == null) {
                startMonth = financialYearStartMonth;
            }
            List<YearMonth> months = new ArrayList<>(12);
            YearMonth cursor = YearMonth.of(startYear, startMonth);
            for (int index = 0; index < 12; index++) {
                months.add(cursor);
                cursor = cursor.plusMonths(1);
            }
            return months;
        }

        return List.of(YearMonth.from(anchorDate));
    }

    public static YearMonth earliestIncludedMonth(AgreementTimePeriod period) {
        if (period == null || period.getIncludedMonths() == null || period.getIncludedMonths().isEmpty()) {
            return null;
        }
        return period.getIncludedMonths().stream()
                .map(TimePeriodDimensions::toYearMonth)
                .filter(Objects::nonNull)
                .min(YearMonth::compareTo)
                .orElse(null);
    }

    public static YearMonth latestIncludedMonth(AgreementTimePeriod period) {
        if (period == null || period.getIncludedMonths() == null || period.getIncludedMonths().isEmpty()) {
            return null;
        }
        return period.getIncludedMonths().stream()
                .map(TimePeriodDimensions::toYearMonth)
                .filter(Objects::nonNull)
                .max(YearMonth::compareTo)
                .orElse(null);
    }

    public static boolean sameIncludedMonths(AgreementTimePeriod left, AgreementTimePeriod right) {
        if (left == null || right == null) {
            return false;
        }
        List<YearMonth> leftMonths = sortedIncludedMonths(left);
        List<YearMonth> rightMonths = sortedIncludedMonths(right);
        return leftMonths.equals(rightMonths);
    }

    public static List<YearMonth> sortedIncludedMonths(AgreementTimePeriod period) {
        if (period == null || period.getIncludedMonths() == null) {
            return List.of();
        }
        return period.getIncludedMonths().stream()
                .map(TimePeriodDimensions::toYearMonth)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    public static YearMonth periodStart(AgreementTimePeriod period) {
        YearMonth earliest = earliestIncludedMonth(period);
        if (earliest == null) {
            throw new IllegalArgumentException("Period is missing temporal dimensions: " + period.getName());
        }
        return earliest;
    }

    public static YearMonth periodEnd(AgreementTimePeriod period) {
        YearMonth latest = latestIncludedMonth(period);
        if (latest == null) {
            throw new IllegalArgumentException("Period is missing temporal dimensions: " + period.getName());
        }
        return latest;
    }

    public static YearMonth periodStart(PayoutFrequency frequency, Integer monthNumber, Integer periodYear) {
        if (periodYear == null || monthNumber == null) {
            throw new IllegalArgumentException("Period is missing temporal dimensions");
        }
        return YearMonth.of(periodYear, monthNumber);
    }

    public static YearMonth periodEnd(PayoutFrequency frequency, YearMonth start) {
        return switch (frequency) {
            case MONTHLY -> start;
            case QUARTERLY -> start.plusMonths(2);
            case HALF_YEARLY -> start.plusMonths(5);
            case YEARLY -> start.plusMonths(11);
            default -> start;
        };
    }

    public static Comparator<AgreementTimePeriod> chronologicalComparator() {
        return AgreementTimePeriod.chronologicalComparator();
    }

    private static YearMonth toYearMonth(AgreementTimePeriodMonth month) {
        if (month == null || month.getCalendarMonth() == null || month.getCalendarYear() == null) {
            return null;
        }
        return YearMonth.of(month.getCalendarYear(), month.getCalendarMonth());
    }

    private static List<YearMonth> assignCalendarYears(
            int[] monthTokens,
            int displayYear,
            int financialYearStartMonth) {
        int firstMonth = monthTokens[0];
        int fyAnchorYear = firstMonth < financialYearStartMonth
                || (financialYearStartMonth != 1 && firstMonth == 1)
                ? displayYear - 1
                : displayYear;
        int calendarYear = firstMonth < financialYearStartMonth ? displayYear : fyAnchorYear;

        List<YearMonth> months = new ArrayList<>(monthTokens.length);
        int year = calendarYear;
        int previousMonth = -1;
        for (int month : monthTokens) {
            if (previousMonth > 0 && month < previousMonth) {
                year++;
            }
            months.add(YearMonth.of(year, month));
            previousMonth = month;
        }
        return months;
    }
}
