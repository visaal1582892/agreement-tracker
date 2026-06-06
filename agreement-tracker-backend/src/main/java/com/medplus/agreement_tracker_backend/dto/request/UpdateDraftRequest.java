package com.medplus.agreement_tracker_backend.dto.request;

import java.util.List;

public record UpdateDraftRequest(
        Long companyId,
        List<Long> vendorIds,
        ProductRulesPayload productRules,
        DraftDetailsPayload details,
        DraftCommercialsPayload commercials
) {}
