package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.dto.request.JbpConfigurationBlockDto;
import com.medplus.agreement_tracker_backend.exception.IncompleteAgreementException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class JbpConfigurationCollisionValidator {

    private JbpConfigurationCollisionValidator() {
    }

    public static void validateNoParentPeriodOverlap(
            List<JbpConfigurationBlockDto> configurations,
            Function<Long, String> periodNameResolver) {
        if (configurations == null || configurations.size() < 2) {
            return;
        }
        Set<Long> claimedPeriodIds = new HashSet<>();
        for (JbpConfigurationBlockDto config : configurations) {
            if (config.parentPeriodIds() == null) {
                continue;
            }
            for (Long periodId : config.parentPeriodIds()) {
                if (!claimedPeriodIds.add(periodId)) {
                    String periodName = periodNameResolver.apply(periodId);
                    throw new IncompleteAgreementException(String.format(
                            "Configuration Collision: The period [%s] cannot be assigned to multiple configurations.",
                            periodName));
                }
            }
        }
    }
}
