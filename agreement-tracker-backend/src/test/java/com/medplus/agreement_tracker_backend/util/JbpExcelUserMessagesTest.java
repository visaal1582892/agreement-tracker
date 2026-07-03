package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JbpExcelUserMessagesTest {

    @Test
    void formatsConciseRowHints() {
        assertEquals(
                "Slab 1: Enter Target Type (ABSOLUTE or RELATIVE).",
                JbpExcelUserMessages.targetTypeRequired("Slab 1"));
        assertEquals(
                "Parent period rows must use Target Type ABSOLUTE.",
                JbpExcelUserMessages.parentTargetMustBeAbsolute());
        assertEquals(
                "Add a Payout greater than 0, or a Qualifier % greater than 0.",
                JbpExcelUserMessages.subPeriodNeedsPayoutOrQualifier());
    }

    @Test
    void normalizesLegacyViolationPrefixes() {
        String normalized = JbpExcelUserMessages.normalizeCaughtMessage(
                "Data Integrity Violation: The Highest Parent interval [Q1] must have a defined Payout.");
        assertEquals("The Highest Parent interval [Q1] must have a defined Payout.", normalized);
    }

    @Test
    void formatsFriendlyReferenceErrors() {
        assertTrue(JbpExcelUserMessages.unknownEntityId(42L).contains("Re-download the template"));
        assertEquals(
                "Period '04-05-06 (2026)' does not belong on a Quarterly sheet.",
                JbpExcelUserMessages.wrongPeriodFrequency("04-05-06 (2026)", PayoutFrequency.QUARTERLY));
    }
}
