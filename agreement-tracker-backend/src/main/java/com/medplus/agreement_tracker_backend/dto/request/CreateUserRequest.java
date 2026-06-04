package com.medplus.agreement_tracker_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100)
        String username,

        @NotBlank(message = "Full name is required")
        @Size(max = 200)
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        String email,

        String employeeId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        List<String> roles
) {}
