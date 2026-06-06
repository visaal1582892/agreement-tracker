package com.medplus.agreement_tracker_backend.dto.request;

public record DraftAgreementItemRequest(
        DraftDetailsPayload details,
        DraftCommercialsPayload commercials
) {}
