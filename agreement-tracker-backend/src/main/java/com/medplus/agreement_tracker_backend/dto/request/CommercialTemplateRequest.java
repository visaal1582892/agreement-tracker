package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CommercialTemplateRequest(
        @NotEmpty List<@NotBlank String> selectedFrequencies
) {}
