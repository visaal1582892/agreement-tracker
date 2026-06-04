package com.medplus.agreement_tracker_backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String employeeId,
        String username,
        String fullName,
        String email,
        boolean isActive,
        List<String> roles,
        LocalDateTime createdAt
) {}
