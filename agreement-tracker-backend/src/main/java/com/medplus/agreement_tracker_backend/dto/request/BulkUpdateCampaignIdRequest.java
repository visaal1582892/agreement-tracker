package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkUpdateCampaignIdRequest(
        @NotEmpty List<Long> ids,
        @Size(max = 100) String campaignId
) {}
