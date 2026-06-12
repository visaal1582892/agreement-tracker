package com.medplus.agreement_tracker_backend.dto.request.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StateMasterRequest {

    @NotBlank(message = "State name is required")
    @Size(max = 255, message = "State name must not exceed 255 characters")
    private String stateName;

    @NotBlank(message = "State code is required")
    @Size(max = 10, message = "State code must not exceed 10 characters")
    private String stateCode;

    private Boolean isActive;
}
