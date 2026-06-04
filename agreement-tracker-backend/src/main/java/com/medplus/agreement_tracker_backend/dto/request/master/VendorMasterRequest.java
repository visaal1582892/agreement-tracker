package com.medplus.agreement_tracker_backend.dto.request.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VendorMasterRequest {
    @Size(max = 50, message = "Vendor code must not exceed 50 characters")
    private String vendorCode;

    @NotBlank(message = "Vendor name is required")
    @Size(max = 255, message = "Vendor name must not exceed 255 characters")
    private String vendorName;

    private Boolean isActive;
}
