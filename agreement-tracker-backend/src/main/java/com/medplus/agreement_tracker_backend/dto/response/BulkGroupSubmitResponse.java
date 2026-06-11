package com.medplus.agreement_tracker_backend.dto.response;

import java.util.List;

public record BulkGroupSubmitResponse(
        int submittedCount,
        List<String> submittedAgreementNames
) {}
