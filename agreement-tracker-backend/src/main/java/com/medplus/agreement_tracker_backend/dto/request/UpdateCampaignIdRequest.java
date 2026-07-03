package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCampaignIdRequest(
        @Size(max = 100) String campaignId
) {}
