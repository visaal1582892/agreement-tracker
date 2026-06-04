package com.medplus.agreement_tracker_backend.dto.request.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DivisionMasterRequest {
    @Size(max = 50)
    private String divisionCode;

    @NotBlank(message = "Division name is required")
    @Size(max = 255)
    private String divisionName;

    @NotNull(message = "Manufacturer is required")
    private Long manufacturerId;

    private Boolean isActive;
}
