package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TerminateAgreementRequest(
        @NotNull(message = "Termination date is required") LocalDate terminationDate,
        @NotBlank(message = "Termination reason is required") String terminationReason
) {}
