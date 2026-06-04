package com.medplus.agreement_tracker_backend.dto.request.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ManufacturerMasterRequest {
    @Size(max = 50)
    private String manufacturerCode;

    @NotBlank(message = "Manufacturer name is required")
    @Size(max = 255)
    private String manufacturerName;

    private Boolean isActive;
}
