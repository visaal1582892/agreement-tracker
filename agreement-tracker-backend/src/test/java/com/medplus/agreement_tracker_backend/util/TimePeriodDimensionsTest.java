package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimePeriodDimensionsTest {

    @Test
    void parsesQuarterIncludedMonthsForAprilFy() {
        List<YearMonth> months = TimePeriodDimensions.includedMonthsFromName(
                "04-05-06 (2026)",
                PayoutFrequency.QUARTERLY,
                4,
                LocalDate.of(2026, 4, 1));

        assertEquals(List.of(
                YearMonth.of(2026, 4),
                YearMonth.of(2026, 5),
                YearMonth.of(2026, 6)), months);
    }

    @Test
    void parsesHalfYearWithCalendarYearWrap() {
        List<YearMonth> months = TimePeriodDimensions.includedMonthsFromName(
                "10-11-12-01-02-03 (2026)",
                PayoutFrequency.HALF_YEARLY,
                4,
                LocalDate.of(2026, 10, 1));

        assertEquals(List.of(
                YearMonth.of(2026, 10),
                YearMonth.of(2026, 11),
                YearMonth.of(2026, 12),
                YearMonth.of(2027, 1),
                YearMonth.of(2027, 2),
                YearMonth.of(2027, 3)), months);
    }

    @Test
    void parsesFinancialYearIncludedMonths() {
        List<YearMonth> months = TimePeriodDimensions.includedMonthsFromName(
                "FY 2026-2027 (Starts Apr)",
                PayoutFrequency.YEARLY,
                4,
                LocalDate.of(2026, 4, 1));

        assertEquals(12, months.size());
        assertEquals(YearMonth.of(2026, 4), months.get(0));
        assertEquals(YearMonth.of(2027, 3), months.get(11));
    }
}
