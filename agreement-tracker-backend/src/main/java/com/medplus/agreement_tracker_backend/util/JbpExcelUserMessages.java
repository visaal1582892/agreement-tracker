package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;

public final class JbpExcelUserMessages {

    private JbpExcelUserMessages() {
    }

    public static String rowHint(String slabTierLabel, String detail) {
        return slabTierLabel + ": " + detail;
    }

    public static String targetTypeMandatory() {
        return "Target Type is mandatory. Please select ABSOLUTE or RELATIVE.";
    }

    public static String targetValueMandatory() {
        return "Target value is mandatory. Please enter a valid number.";
    }

    public static String invalidNumberFormat(String fieldLabel) {
        return fieldLabel + " has invalid data. Enter a valid number.";
    }

    public static String invalidRowFormat() {
        return "Invalid data format in this row. Check number and dropdown fields.";
    }

    public static String targetTypeRequired(String slabTierLabel) {
        return rowHint(slabTierLabel, "Enter Target Type (ABSOLUTE or RELATIVE).");
    }

    public static String targetRequired(String slabTierLabel) {
        return rowHint(slabTierLabel, "Enter a Target value.");
    }

    public static String parentTargetMustBeAbsolute() {
        return "Parent period rows must use Target Type ABSOLUTE.";
    }

    public static String parentPayoutRequired() {
        return "Enter Payout Type and Payout. Both are required on parent period rows.";
    }

    public static String subPeriodNeedsPayoutOrQualifier() {
        return "Add a Payout greater than 0, or a Qualifier % greater than 0.";
    }

    public static String qualifierTooHigh(String slabTierLabel) {
        return rowHint(slabTierLabel, "Qualifier % cannot be greater than 100.");
    }

    public static String relativeTargetTooHigh(String slabTierLabel) {
        return rowHint(slabTierLabel, "Relative Target cannot be greater than 100.");
    }

    public static String relativePayoutTooHigh(String slabTierLabel) {
        return rowHint(slabTierLabel, "Relative Payout cannot be greater than 100.");
    }

    public static String targetMustIncrease(
            String slabTierLabel,
            String previousSlabTierLabel,
            String previousTarget,
            String currentTarget) {
        return rowHint(
                slabTierLabel,
                "Target (" + currentTarget + ") must be higher than "
                        + previousSlabTierLabel + " (" + previousTarget + ").");
    }

    public static String invalidValueType(String label) {
        return label + " must be ABSOLUTE or RELATIVE.";
    }

    public static String invalidSlabTier(String tierLabel) {
        return "Slab Tier is invalid. Use a value like 'Slab 1'. Found: '" + tierLabel + "'.";
    }

    public static String unknownEntityId(Long entityId) {
        return "Period reference is invalid (Entity ID " + entityId + "). "
                + "Re-download the template and do not change hidden columns.";
    }

    public static String entityPeriodMismatch(Long entityId, String periodName) {
        return "Period '" + periodName + "' does not match Entity ID " + entityId + ". "
                + "Do not edit hidden columns or period names.";
    }

    public static String wrongPeriodFrequency(String periodName, PayoutFrequency frequency) {
        return "Period '" + periodName + "' does not belong on a " + formatFrequency(frequency) + " sheet.";
    }

    public static String unknownParentPeriod(String parentPeriodName) {
        return "Parent Period '" + parentPeriodName + "' was not found. "
                + "Copy parent period names exactly from the template.";
    }

    public static String invalidConfigurationId(Long configurationId) {
        return "Configuration reference is invalid (Config ID " + configurationId + "). "
                + "Re-download the template for this agreement.";
    }

    public static String invalidSlabForConfiguration(Long configurationId, int tierNumber) {
        return "Slab " + tierNumber + " is not allowed for Config ID " + configurationId + ". "
                + "Check the slab count in the template.";
    }

    public static String normalizeCaughtMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "This row could not be validated. Check all required fields.";
        }

        String message = rawMessage.trim();
        if (message.startsWith("Threshold Error in ")) {
            return simplifyLegacyThresholdMessage(message);
        }
        if (message.startsWith("Data Integrity Violation:")
                || message.startsWith("Business Rule Violation:")) {
            return message.substring(message.indexOf(':') + 1).trim();
        }
        return message;
    }

    private static String simplifyLegacyThresholdMessage(String message) {
        int colonIndex = message.lastIndexOf(':');
        if (colonIndex < 0 || colonIndex == message.length() - 1) {
            return message;
        }
        String prefix = message.substring("Threshold Error in ".length(), colonIndex);
        String detail = message.substring(colonIndex + 1).trim();
        int slabStart = prefix.lastIndexOf('(');
        if (slabStart > 0 && prefix.endsWith(")")) {
            String period = prefix.substring(0, slabStart).trim();
            String slab = prefix.substring(slabStart + 1, prefix.length() - 1).trim();
            return slab + " (" + period + "): " + detail;
        }
        return detail;
    }

    private static String formatFrequency(PayoutFrequency frequency) {
        return switch (frequency) {
            case YEARLY -> "Yearly";
            case HALF_YEARLY -> "Half-yearly";
            case QUARTERLY -> "Quarterly";
            case MONTHLY -> "Monthly";
            default -> frequency.name();
        };
    }
}
