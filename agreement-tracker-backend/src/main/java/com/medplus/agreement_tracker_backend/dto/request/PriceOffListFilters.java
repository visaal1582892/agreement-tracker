package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.enums.PriceOffDisplayStatus;
import com.medplus.agreement_tracker_backend.enums.PriceOffDiscountType;

public record PriceOffListFilters(
        String product,
        String campaignId,
        String location,
        String channel,
        PriceOffDiscountType discountType,
        PriceOffDisplayStatus status
) {}
