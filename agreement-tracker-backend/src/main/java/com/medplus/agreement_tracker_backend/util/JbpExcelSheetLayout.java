package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;

public final class JbpExcelSheetLayout {

    public static final String[] VALUE_TYPE_OPTIONS = {"ABSOLUTE", "RELATIVE"};
    public static final String[] ABSOLUTE_ONLY_OPTIONS = {"ABSOLUTE"};
    public static final String[] OPTIONAL_VALUE_TYPE_OPTIONS = {"", "ABSOLUTE", "RELATIVE"};

    private JbpExcelSheetLayout() {
    }

    public record Columns(
            int colEntityId,
            int colParentPeriod,
            int colSubPeriod,
            int colSlabTier,
            int colTargetType,
            int colTarget,
            int colQualifierPercent,
            int colPayoutType,
            int colPayout,
            int colMaxPurchase,
            int colMaxPayout,
            int colConfigId,
            boolean master,
            String[] headers) {

        public int editableStartCol() {
            return colTargetType;
        }

        public int editableEndCol() {
            return colMaxPayout;
        }

        public int colPeriod() {
            return master ? colParentPeriod : colSubPeriod;
        }
    }

    public static Columns forSheet(boolean master) {
        return master ? masterThreshold() : spreadThreshold();
    }

    public static String canonicalMasterSheetName(PayoutFrequency frequency) {
        return "Master_" + frequency.name();
    }

    public static String canonicalSpreadSheetName(PayoutFrequency frequency) {
        return "Spread_" + frequency.name();
    }

    private static Columns masterThreshold() {
        return new Columns(
                0, 1, -1, 2, 3, 4, 5, 6, 7, 8, 9, 10, true,
                new String[]{
                        "Entity ID", "Period", "Slab Tier", "Target Type", "Target", "Qualifier %",
                        "Payout Type", "Payout", "Max Purchase (Optional)", "Max Payout (Optional)", "Config ID"
                });
    }

    private static Columns spreadThreshold() {
        return new Columns(
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, false,
                new String[]{
                        "Entity ID", "Parent Period", "Sub Period", "Slab Tier", "Target Type", "Target",
                        "Qualifier %", "Payout Type (Optional)", "Payout (Optional)", "Max Purchase (Optional)", "Max Payout (Optional)",
                        "Config ID"
                });
    }
}
