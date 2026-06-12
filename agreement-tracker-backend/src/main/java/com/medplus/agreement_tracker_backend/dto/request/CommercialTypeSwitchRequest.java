package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import com.medplus.agreement_tracker_backend.enums.CommercialTypeSwitchAction;
import jakarta.validation.constraints.NotNull;

public record CommercialTypeSwitchRequest(
        @NotNull CommercialTypeSwitchAction action,
        @NotNull CommercialSlabType newSlabType
) {}
