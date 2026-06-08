package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record InitiateTerminateRequest(
        @NotBlank(message = "Reason / comments are required")
        @Size(max = 1000)
        String comments,
        @PastOrPresent(message = "Termination date cannot be in the future")
        LocalDate requestedTerminationDate
) {}
