package com.medplus.agreement_tracker_backend.dto.response;

import java.time.LocalDateTime;

public record CompanyAgreementGroupResponse(
        Long id,
        Long companyId,
        String companyName,
        String name,
        boolean isActive,
        Long createdByUserId,
        String createdByName,
        LocalDateTime lastModifiedAt,
        String lastModifiedByName
) {}
