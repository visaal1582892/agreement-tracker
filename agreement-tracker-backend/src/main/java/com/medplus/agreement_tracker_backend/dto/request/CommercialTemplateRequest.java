package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CommercialTemplateRequest(
        @NotEmpty List<@NotBlank String> selectedFrequencies,
        CommercialSlabType slabType
) {}
