package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record RuleDTO(
        @NotNull(message = "Rule id is required") Long id,
        @NotNull(message = "Rule type is required") String ruleType
) {}
