package com.medplus.agreement_tracker_backend.dto.request.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class IncomeTypeRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 255)
    private String description;

    private Boolean isActive;
}
