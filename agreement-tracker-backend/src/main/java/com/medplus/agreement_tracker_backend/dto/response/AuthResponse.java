package com.medplus.agreement_tracker_backend.dto.response;

import java.util.List;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        String fullName,
        String email,
        List<String> roles,
        List<String> rights
) {
    public AuthResponse(String token, Long userId, String username, String fullName, String email,
                        List<String> roles, List<String> rights) {
        this(token, "Bearer", userId, username, fullName, email, roles, rights);
    }
}
