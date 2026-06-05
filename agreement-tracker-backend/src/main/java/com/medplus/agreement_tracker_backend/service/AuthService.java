package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.LoginRequest;
import com.medplus.agreement_tracker_backend.dto.response.AuthResponse;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    /** Returns current user roles and rights (for CAS / session refresh). */
    AuthResponse getSession(UserPrincipal principal);
}
