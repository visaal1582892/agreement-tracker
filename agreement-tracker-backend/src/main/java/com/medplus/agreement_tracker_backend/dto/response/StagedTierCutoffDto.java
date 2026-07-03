package com.medplus.agreement_tracker_backend.dto.response;

import java.math.BigDecimal;

public record StagedTierCutoffDto(
        Long slabId,
        BigDecimal lowerCutoff,
        BigDecimal upperCutoff
) {}
