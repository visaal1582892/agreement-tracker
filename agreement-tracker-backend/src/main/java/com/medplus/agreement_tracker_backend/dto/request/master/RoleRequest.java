package com.medplus.agreement_tracker_backend.dto.request.master;

import com.medplus.agreement_tracker_backend.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleRequest {
    @NotNull(message = "Role name is required")
    private RoleName name;

    @Size(max = 255)
    private String description;

    private Boolean isActive;
}
