package com.medplus.agreement_tracker_backend.dto.request;

import java.util.List;

public record ProductRulesPayload(
        List<Long> manufacturers,

        List<RuleDTO> divisionRules,

        List<RuleDTO> productRules
) {}
