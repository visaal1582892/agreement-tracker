package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.enums.AssetCategory;

import java.math.BigDecimal;
import java.util.List;

public record DraftAssetPayload(
        AssetCategory assetCategory,
        String assetType,
        Integer storeCount,
        BigDecimal payoutPerStore,
        BigDecimal flatPayout,
        String remarks,
        List<AssetPayoutPeriodDto> assetPayoutPeriods
) {}
