package com.medplus.agreement_tracker_backend.dto.request.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RightRequest {
    @NotBlank(message = "Code is required")
    @Size(max = 100)
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 100)
    private String module;

    @Size(max = 255)
    private String description;

    private Boolean isActive;
}
