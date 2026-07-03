package com.medplus.agreement_tracker_backend.enums;

import java.util.Locale;

public enum PriceOffDiscountType {
    DISC_VAL("Disc Val"),
    DISC_PERCENT("Disc %");

    private final String label;

    PriceOffDiscountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PriceOffDiscountType fromLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Discount type is required");
        }
        String normalized = raw.trim();
        for (PriceOffDiscountType type : values()) {
            if (type.label.equalsIgnoreCase(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Discount type '" + raw + "' is not valid");
    }
}
