package com.medplus.agreement_tracker_backend.dto.request.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductMasterRequest {
    @Size(max = 50)
    private String productCode;

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    private String productName;

    @NotNull(message = "Manufacturer is required")
    private Long manufacturerId;

    @NotNull(message = "Division is required")
    private Long divisionId;

    private Boolean isActive;
}
