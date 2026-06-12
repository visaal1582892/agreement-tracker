package com.medplus.agreement_tracker_backend.enums;

public enum CommercialSlabType {
    SALE,
    PURCHASE;

    public CommercialSlabType inverseTargetType() {
        return this == SALE ? PURCHASE : SALE;
    }
}
