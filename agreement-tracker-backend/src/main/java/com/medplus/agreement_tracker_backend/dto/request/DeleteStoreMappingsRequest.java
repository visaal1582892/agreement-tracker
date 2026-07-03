package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DeleteStoreMappingsRequest(
        @NotEmpty List<Long> mappingIds
) {}
