package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriodMonth;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JbpTemporalReconciliationUtilTest {

    @Test
    void firstHalfYearResolvesFirstTwoQuartersForAprilFy() {
        AgreementTimePeriod h1 = period("04-05-06-07-08-09 (2026)", PayoutFrequency.HALF_YEARLY,
                List.of(4, 5, 6, 7, 8, 9), 2026);
        AgreementTimePeriod q1 = period("04-05-06 (2026)", PayoutFrequency.QUARTERLY,
                List.of(4, 5, 6), 2026);
        AgreementTimePeriod q2 = period("07-08-09 (2026)", PayoutFrequency.QUARTERLY,
                List.of(7, 8, 9), 2026);
        AgreementTimePeriod q3 = period("10-11-12 (2026)", PayoutFrequency.QUARTERLY,
                List.of(10, 11, 12), 2026);

        List<AgreementTimePeriod> matched = JbpTemporalReconciliationUtil.filterSubPeriodsForParent(h1, List.of(q1, q2, q3));

        assertEquals(2, matched.size());
        assertEquals("04-05-06 (2026)", matched.get(0).getName());
        assertEquals("07-08-09 (2026)", matched.get(1).getName());
    }

    @Test
    void secondHalfYearResolvesLastTwoQuartersForAprilFy() {
        AgreementTimePeriod h2 = period("10-11-12-01-02-03 (2026)", PayoutFrequency.HALF_YEARLY,
                List.of(10, 11, 12, 1, 2, 3), List.of(2026, 2026, 2026, 2027, 2027, 2027));
        AgreementTimePeriod q3 = period("10-11-12 (2026)", PayoutFrequency.QUARTERLY,
                List.of(10, 11, 12), 2026);
        AgreementTimePeriod q4 = period("01-02-03 (2027)", PayoutFrequency.QUARTERLY,
                List.of(1, 2, 3), 2027);

        List<AgreementTimePeriod> matched = JbpTemporalReconciliationUtil.filterSubPeriodsForParent(h2, List.of(q3, q4));

        assertEquals(2, matched.size());
        assertEquals("10-11-12 (2026)", matched.get(0).getName());
        assertEquals("01-02-03 (2027)", matched.get(1).getName());
    }

    @Test
    void spreadFrequencyPairing() {
        assertTrue(JbpTemporalReconciliationUtil.isSpreadFrequencyOf(
                PayoutFrequency.HALF_YEARLY, PayoutFrequency.QUARTERLY));
    }

    private static AgreementTimePeriod period(
            String name,
            PayoutFrequency frequency,
            List<Integer> months,
            int year) {
        return period(name, frequency, months, months.stream().map(ignored -> year).toList());
    }

    private static AgreementTimePeriod period(
            String name,
            PayoutFrequency frequency,
            List<Integer> months,
            List<Integer> years) {
        AgreementTimePeriod period = AgreementTimePeriod.builder()
                .name(name)
                .periodFrequency(frequency)
                .build();
        for (int index = 0; index < months.size(); index++) {
            AgreementTimePeriodMonth month = AgreementTimePeriodMonth.builder()
                    .timePeriod(period)
                    .calendarMonth(months.get(index))
                    .calendarYear(years.get(index))
                    .build();
            period.getIncludedMonths().add(month);
        }
        return period;
    }
}
