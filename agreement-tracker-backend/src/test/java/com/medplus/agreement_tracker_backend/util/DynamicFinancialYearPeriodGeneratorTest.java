package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicFinancialYearPeriodGeneratorTest {

    @Test
    void generatesAprilFyQuarterNames() {
        List<String> quarters = DynamicFinancialYearPeriodGenerator.generateQuarterlyPeriods(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2027, 3, 31),
                4);

        assertEquals(4, quarters.size());
        assertEquals("04-05-06 (2026)", quarters.get(0));
        assertEquals("07-08-09 (2026)", quarters.get(1));
        assertEquals("10-11-12 (2026)", quarters.get(2));
        assertEquals("01-02-03 (2027)", quarters.get(3));
    }

    @Test
    void generatesJanuaryFyQuarterNames() {
        List<String> quarters = DynamicFinancialYearPeriodGenerator.generateQuarterlyPeriods(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                1);

        assertEquals(4, quarters.size());
        assertEquals("01-02-03 (2026)", quarters.get(0));
        assertEquals("04-05-06 (2026)", quarters.get(1));
        assertEquals("07-08-09 (2026)", quarters.get(2));
        assertEquals("10-11-12 (2026)", quarters.get(3));
    }

    @Test
    void generatesNovemberFyFirstQuarterWithYearWrap() {
        List<String> quarters = DynamicFinancialYearPeriodGenerator.generateQuarterlyPeriods(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 10, 31),
                11);

        assertEquals(4, quarters.size());
        assertEquals("11-12-01 (2026)", quarters.get(0));
        assertEquals("02-03-04 (2027)", quarters.get(1));
        assertEquals("05-06-07 (2027)", quarters.get(2));
        assertEquals("08-09-10 (2027)", quarters.get(3));
    }

    @Test
    void generatesAprilFyYearName() {
        List<String> years = DynamicFinancialYearPeriodGenerator.generateYearlyPeriods(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2027, 3, 31),
                4);

        assertEquals(1, years.size());
        assertEquals("FY 2026-2027 (Starts Apr)", years.get(0));
    }

    @Test
    void generatesJanuaryFyYearName() {
        List<String> years = DynamicFinancialYearPeriodGenerator.generateYearlyPeriods(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                1);

        assertEquals(1, years.size());
        assertEquals("FY 2026-2027 (Starts Jan)", years.get(0));
    }

    @Test
    void generatesMonthlyNames() {
        List<String> months = DynamicFinancialYearPeriodGenerator.generateMonthlyPeriods(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30));

        assertEquals(List.of("04-2026", "05-2026", "06-2026"), months);
    }

    @Test
    void masterSheetLayoutHasThresholdColumns() {
        var layout = JbpExcelSheetLayout.forSheet(true);
        assertTrue(layout.master());
        assertEquals("Target Type", layout.headers()[3]);
        assertEquals("Max Payout (Optional)", layout.headers()[9]);
    }
}
